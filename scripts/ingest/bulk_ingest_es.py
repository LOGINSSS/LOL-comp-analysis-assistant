#!/usr/bin/env python3
"""Bulk-ingest JSONL match docs into Elasticsearch index `lol_matches`.

Input format: one JSON object per line (document body). `_id` is generated as
`{matchId}_{side}` for idempotent upserts.
"""

from __future__ import annotations

import argparse
import json
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any, Dict, Iterable, List, Tuple

INDEX_MAPPING = {
    "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0,
        "refresh_interval": "5s",
    },
    "mappings": {
        "dynamic": "strict",
        "properties": {
            "matchId": {"type": "keyword"},
            "side": {"type": "keyword"},
            "patch": {"type": "keyword"},
            "region": {"type": "keyword"},
            "queue": {"type": "keyword"},
            "rankTier": {"type": "keyword"},
            "timestamp": {"type": "date"},
            "win": {"type": "boolean"},
            "ally": {
                "properties": {
                    "TOP": {"type": "integer"},
                    "JUNGLE": {"type": "integer"},
                    "MID": {"type": "integer"},
                    "ADC": {"type": "integer"},
                    "SUP": {"type": "integer"},
                }
            },
            "enemy": {
                "properties": {
                    "TOP": {"type": "integer"},
                    "JUNGLE": {"type": "integer"},
                    "MID": {"type": "integer"},
                    "ADC": {"type": "integer"},
                    "SUP": {"type": "integer"},
                }
            },
            "allyTeam": {"type": "integer"},
            "enemyTeam": {"type": "integer"},
        },
    },
}


def http_json(method: str, url: str, *, body: bytes | None = None, content_type: str | None = None) -> Any:
    headers: Dict[str, str] = {}
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(url=url, method=method, data=body, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode("utf-8")
            if not raw:
                return None
            return json.loads(raw)
    except urllib.error.HTTPError as exc:
        err_body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exc.code} {url}\n{err_body}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Network error calling {url}: {exc}") from exc


def http_status(method: str, url: str) -> int:
    req = urllib.request.Request(url=url, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status
    except urllib.error.HTTPError as exc:
        return exc.code


def ensure_index(es_url: str, index: str) -> None:
    code = http_status("HEAD", f"{es_url}/{index}")
    if code == 200:
        return
    if code != 404:
        raise RuntimeError(f"Unexpected status checking index {index}: {code}")
    body = json.dumps(INDEX_MAPPING).encode("utf-8")
    http_json("PUT", f"{es_url}/{index}", body=body, content_type="application/json")


def read_docs(input_file: Path) -> Iterable[Dict[str, Any]]:
    with input_file.open("r", encoding="utf-8") as f:
        for ln, line in enumerate(f, start=1):
            txt = line.strip()
            if not txt:
                continue
            try:
                doc = json.loads(txt)
            except json.JSONDecodeError as exc:
                raise RuntimeError(f"Invalid JSON at line {ln}: {exc}") from exc
            if not isinstance(doc, dict):
                raise RuntimeError(f"Line {ln} is not an object")
            yield doc


def to_bulk_payload(index: str, docs: List[Dict[str, Any]]) -> bytes:
    lines: List[str] = []
    for doc in docs:
        match_id = doc.get("matchId")
        side = doc.get("side")
        if not match_id or not side:
            raise RuntimeError("Each doc must contain matchId and side")
        doc_id = f"{match_id}_{side}"
        lines.append(json.dumps({"index": {"_index": index, "_id": doc_id}}, ensure_ascii=True))
        lines.append(json.dumps(doc, ensure_ascii=True))
    payload = "\n".join(lines) + "\n"
    return payload.encode("utf-8")


def bulk_write(es_url: str, index: str, docs: List[Dict[str, Any]], refresh: str | None) -> Tuple[int, int]:
    if not docs:
        return 0, 0
    query = f"?refresh={refresh}" if refresh else ""
    payload = to_bulk_payload(index, docs)
    result = http_json(
        "POST",
        f"{es_url}/_bulk{query}",
        body=payload,
        content_type="application/x-ndjson",
    )

    items = result.get("items", []) if isinstance(result, dict) else []
    errors = 0
    successes = 0
    for item in items:
        action = item.get("index", {})
        status = int(action.get("status", 500))
        if 200 <= status < 300:
            successes += 1
        else:
            errors += 1

    if isinstance(result, dict) and result.get("errors") and errors == 0:
        # Defensive fallback for unusual response shapes.
        errors = 1

    return successes, errors


def count_docs(es_url: str, index: str) -> int:
    result = http_json("GET", f"{es_url}/{index}/_count")
    if not isinstance(result, dict):
        return 0
    return int(result.get("count", 0))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Bulk ingest JSONL docs into Elasticsearch")
    parser.add_argument("--es-url", default="http://localhost:9200", help="Elasticsearch base URL")
    parser.add_argument("--index", default="lol_matches", help="Target index")
    parser.add_argument("--input", required=True, help="Input JSONL file path")
    parser.add_argument("--batch-size", type=int, default=1000, help="Docs per bulk request")
    parser.add_argument("--refresh", default="wait_for", help="Bulk refresh mode: wait_for/true/false")
    parser.add_argument("--ensure-index", action="store_true", help="Create index if missing")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    es_url = args.es_url.rstrip("/")
    input_file = Path(args.input)

    if not input_file.exists():
        raise RuntimeError(f"Input file not found: {input_file}")

    info = http_json("GET", f"{es_url}")
    version = (info or {}).get("version", {}).get("number", "unknown") if isinstance(info, dict) else "unknown"
    print(f"Connected to ES: {es_url} (version={version})")

    if args.ensure_index:
        ensure_index(es_url, args.index)
        print(f"Index ensured: {args.index}")

    buffered: List[Dict[str, Any]] = []
    total_success = 0
    total_error = 0

    for doc in read_docs(input_file):
        buffered.append(doc)
        if len(buffered) >= args.batch_size:
            ok, err = bulk_write(es_url, args.index, buffered, args.refresh)
            total_success += ok
            total_error += err
            buffered.clear()

    if buffered:
        ok, err = bulk_write(es_url, args.index, buffered, args.refresh)
        total_success += ok
        total_error += err

    total_count = count_docs(es_url, args.index)
    print(f"Bulk success docs: {total_success}")
    print(f"Bulk error docs: {total_error}")
    print(f"Current {args.index} docs.count: {total_count}")

    if total_success == 0:
        print("WARNING: zero documents written. Please inspect collection output and bulk response.")
    return 0 if total_error == 0 else 2


if __name__ == "__main__":
    raise SystemExit(main())

