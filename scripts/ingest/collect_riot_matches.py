#!/usr/bin/env python3
"""Collect Riot match details and transform them into ES-ready JSONL documents.

One match is expanded into two team-perspective docs (RED/BLUE), compatible with
index `lol_matches` mapping in `es-schema.md`.
"""

from __future__ import annotations

import argparse
from collections import deque
import json
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any, Dict, Iterable, List

VALID_ROUTING_REGIONS = {"asia", "europe", "americas", "sea"}

ROLE_MAP = {
    "TOP": "TOP",
    "JUNGLE": "JUNGLE",
    "MIDDLE": "MID",
    "MID": "MID",
    "BOTTOM": "ADC",
    "ADC": "ADC",
    "UTILITY": "SUP",
    "SUPPORT": "SUP",
    "SUP": "SUP",
}


class DualWindowRateLimiter:
    """Riot dev-key limiter: max N requests per second and per 2-minute window."""

    def __init__(self, per_second: int, per_two_minutes: int) -> None:
        self.per_second = per_second
        self.per_two_minutes = per_two_minutes
        self._sec_window: deque[float] = deque()
        self._two_min_window: deque[float] = deque()

    def _evict(self, now: float) -> None:
        while self._sec_window and now - self._sec_window[0] >= 1.0:
            self._sec_window.popleft()
        while self._two_min_window and now - self._two_min_window[0] >= 120.0:
            self._two_min_window.popleft()

    def acquire(self) -> None:
        while True:
            now = time.monotonic()
            self._evict(now)

            sec_limited = len(self._sec_window) >= self.per_second
            two_min_limited = len(self._two_min_window) >= self.per_two_minutes
            if not sec_limited and not two_min_limited:
                self._sec_window.append(now)
                self._two_min_window.append(now)
                return

            wait_seconds = 0.01
            if sec_limited:
                wait_seconds = max(wait_seconds, 1.0 - (now - self._sec_window[0]))
            if two_min_limited:
                wait_seconds = max(wait_seconds, 120.0 - (now - self._two_min_window[0]))
            time.sleep(wait_seconds)


def http_json(
    method: str,
    url: str,
    *,
    headers: Dict[str, str] | None = None,
    limiter: DualWindowRateLimiter | None = None,
) -> Any:
    if limiter is not None:
        limiter.acquire()
    req = urllib.request.Request(url=url, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode("utf-8")
            if not raw:
                return None
            return json.loads(raw)
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        hint = ""
        if exc.code == 403:
            tips = [
                "- Check --api-key: dev keys expire quickly and must be regenerated.",
                "- Ensure --puuid is the real value (not <PUUID> placeholder).",
                "- Ensure --region is one of: asia/europe/americas/sea.",
                "- If error code 1010 appears, retry without proxy/VPN or from another network.",
            ]
            hint = "\nTroubleshooting:\n" + "\n".join(tips)
        raise RuntimeError(f"HTTP {exc.code} {url}\n{body}{hint}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Network error calling {url}: {exc}") from exc


def _is_placeholder(value: str) -> bool:
    v = value.strip()
    return v.startswith("<") and v.endswith(">")


def validate_args(args: argparse.Namespace) -> argparse.Namespace:
    if _is_placeholder(args.api_key):
        raise ValueError("--api-key still looks like a placeholder. Replace <RIOT_API_KEY> with your real key.")
    if _is_placeholder(args.puuid):
        raise ValueError("--puuid still looks like a placeholder. Replace <PUUID> with a real player PUUID.")

    args.region = args.region.lower().strip()
    if args.region not in VALID_ROUTING_REGIONS:
        raise ValueError("--region must be one of: asia, europe, americas, sea")

    if args.count < 1 or args.count > 100:
        raise ValueError("--count must be in range [1, 100]")
    if args.start < 0:
        raise ValueError("--start must be >= 0")
    if args.max_req_per_sec < 1:
        raise ValueError("--max-req-per-sec must be >= 1")
    if args.max_req_per_2min < 1:
        raise ValueError("--max-req-per-2min must be >= 1")

    return args


def normalize_patch(game_version: str | None) -> str:
    if not game_version:
        return "unknown"
    parts = game_version.split(".")
    if len(parts) >= 2:
        return f"{parts[0]}.{parts[1]}"
    return game_version


def infer_region_from_match_id(match_id: str) -> str:
    if not match_id:
        return "UNKNOWN"
    return match_id.split("_", 1)[0]


def normalize_role(team_position: str | None, lane: str | None, individual_position: str | None) -> str | None:
    candidates = [team_position, individual_position, lane]
    for raw in candidates:
        if not raw:
            continue
        role = ROLE_MAP.get(raw.upper())
        if role:
            return role
    return None


def build_role_map(participants: Iterable[Dict[str, Any]], team_id: int) -> Dict[str, int]:
    result: Dict[str, int] = {}
    for p in participants:
        if p.get("teamId") != team_id:
            continue
        role = normalize_role(
            p.get("teamPosition"),
            p.get("lane"),
            p.get("individualPosition"),
        )
        champion_id = p.get("championId")
        if role and isinstance(champion_id, int):
            result[role] = champion_id
    return result


def build_team_list(role_map: Dict[str, int]) -> List[int]:
    # Stable ordering avoids noise in diffs/debug output.
    order = ["TOP", "JUNGLE", "MID", "ADC", "SUP"]
    return [role_map[r] for r in order if r in role_map]


def team_win_by_id(teams: List[Dict[str, Any]], team_id: int) -> bool:
    for t in teams:
        if t.get("teamId") == team_id:
            return bool(t.get("win", False))
    return False


def to_perspective_docs(match_detail: Dict[str, Any]) -> List[Dict[str, Any]]:
    metadata = match_detail.get("metadata", {})
    info = match_detail.get("info", {})

    match_id = metadata.get("matchId", "")
    participants = info.get("participants", [])
    teams = info.get("teams", [])

    red_map = build_role_map(participants, 100)
    blue_map = build_role_map(participants, 200)

    patch = normalize_patch(info.get("gameVersion"))
    region = infer_region_from_match_id(match_id)
    queue = str(info.get("queueId", "UNKNOWN"))
    timestamp = int(info.get("gameEndTimestamp") or info.get("gameStartTimestamp") or 0)

    red_doc = {
        "matchId": match_id,
        "side": "RED",
        "patch": patch,
        "region": region,
        "queue": queue,
        "rankTier": "UNKNOWN",
        "timestamp": timestamp,
        "win": team_win_by_id(teams, 100),
        "ally": red_map,
        "enemy": blue_map,
        "allyTeam": build_team_list(red_map),
        "enemyTeam": build_team_list(blue_map),
    }

    blue_doc = {
        "matchId": match_id,
        "side": "BLUE",
        "patch": patch,
        "region": region,
        "queue": queue,
        "rankTier": "UNKNOWN",
        "timestamp": timestamp,
        "win": team_win_by_id(teams, 200),
        "ally": blue_map,
        "enemy": red_map,
        "allyTeam": build_team_list(blue_map),
        "enemyTeam": build_team_list(red_map),
    }

    return [red_doc, blue_doc]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Collect Riot matches and write ES-ready docs as JSONL.")
    parser.add_argument("--api-key", required=True, help="Riot API key")
    parser.add_argument("--region", required=True, help="Riot routing region: asia/europe/americas/sea")
    parser.add_argument("--puuid", required=True, help="Player PUUID")
    parser.add_argument("--start", type=int, default=0, help="Pagination start index")
    parser.add_argument("--count", type=int, default=20, help="Number of matches to fetch (max 100)")
    parser.add_argument("--queue", type=int, default=None, help="Optional queue id filter")
    parser.add_argument("--type", dest="match_type", default=None, help="Optional match type: ranked/normal/tourney")
    parser.add_argument("--sleep-ms", type=int, default=120, help="Sleep milliseconds between detail requests")
    parser.add_argument("--max-req-per-sec", type=int, default=20, help="Hard rate limit: max requests per second")
    parser.add_argument("--max-req-per-2min", type=int, default=100, help="Hard rate limit: max requests per 2 minutes")
    parser.add_argument("--out", required=True, help="Output JSONL path")
    return parser.parse_args()


def main() -> int:
    args = validate_args(parse_args())
    limiter = DualWindowRateLimiter(args.max_req_per_sec, args.max_req_per_2min)
    # Explicit headers reduce provider-side blocking of default urllib fingerprints.
    headers = {
        "X-Riot-Token": args.api_key,
        "Accept": "application/json",
        "User-Agent": "LOLCAA-Ingest/0.1 (Windows; Python urllib)",
    }

    query: Dict[str, Any] = {"start": args.start, "count": args.count}
    if args.queue is not None:
        query["queue"] = args.queue
    if args.match_type:
        query["type"] = args.match_type

    qs = urllib.parse.urlencode(query)
    ids_url = (
        f"https://{args.region}.api.riotgames.com/lol/match/v5/matches/"
        f"by-puuid/{urllib.parse.quote(args.puuid)}/ids?{qs}"
    )

    match_ids = http_json("GET", ids_url, headers=headers, limiter=limiter)
    if not isinstance(match_ids, list):
        raise RuntimeError("Unexpected response for match id list")

    output_path = Path(args.out)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    doc_count = 0
    with output_path.open("w", encoding="utf-8") as out:
        for i, match_id in enumerate(match_ids, start=1):
            detail_url = f"https://{args.region}.api.riotgames.com/lol/match/v5/matches/{urllib.parse.quote(str(match_id))}"
            detail = http_json("GET", detail_url, headers=headers, limiter=limiter)
            docs = to_perspective_docs(detail)
            for doc in docs:
                out.write(json.dumps(doc, ensure_ascii=True) + "\n")
                doc_count += 1
            if i < len(match_ids) and args.sleep_ms > 0:
                time.sleep(args.sleep_ms / 1000.0)

    print(f"Fetched matches: {len(match_ids)}")
    print(f"Generated docs: {doc_count}")
    print(f"Output: {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

