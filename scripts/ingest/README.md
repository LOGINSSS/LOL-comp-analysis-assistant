# Ingest Scripts (`lol_matches`)

This folder provides a minimal collection + ingestion pipeline:

0. `get_puuid.ps1`
   - Looks up `puuid` from Riot ID (`gameName#tagLine`) via Account-V1
   - Optional `-SetEnv` to export `RIOT_PUUID` in current shell session

0. `collect_100_matches.ps1`
   - One-command collection for 100 matches using a prefilled PUUID
   - Uses strict rate-limit args (`20 req/s`, `100 req/2min`)

1. `collect_riot_matches.py`
   - Pulls match IDs and match details from Riot Match-V5
   - Converts each match into **2 documents** (`RED` / `BLUE` perspective)
   - Writes JSONL output for bulk loading

2. `bulk_ingest_es.py`
   - Optionally creates `lol_matches` index with strict mapping
   - Bulk writes JSONL docs with idempotent `_id={matchId}_{side}`
   - Prints `docs.count` after ingestion

## 1) Collect

Quick one-command run (prefilled PUUID, 100 matches):

```powershell
$env:RIOT_API_KEY = "RGAPI-<YOUR_NEW_KEY>"
.\scripts\ingest\collect_100_matches.ps1
```

Optional custom output path:

```powershell
.\scripts\ingest\collect_100_matches.ps1 -OutFile "scripts/ingest/out/matches_batch1.jsonl"
```

Optional quick step to resolve `puuid` from Riot ID:

```powershell
$apiKey = "RGAPI-<YOUR_NEW_KEY>"
.\scripts\ingest\get_puuid.ps1 -ApiKey $apiKey -RiotId "gameName#tagLine" -Region asia -SetEnv
```

You can also pass split fields:

```powershell
.\scripts\ingest\get_puuid.ps1 -ApiKey $apiKey -GameName "gameName" -TagLine "tagLine" -Region asia -SetEnv
```

Before running, replace placeholders with real values:
- `<RIOT_API_KEY>`: your current Riot API key
- `<PUUID>`: a real player PUUID

```powershell
python scripts/ingest/collect_riot_matches.py --api-key "<RIOT_API_KEY>" --region asia --puuid "<PUUID>" --start 0 --count 20 --out "scripts/ingest/out/matches.jsonl"
```

Optional filters:
- `--queue 420` (ranked solo)
- `--type ranked`

## 2) Bulk ingest into ES

```powershell
python scripts/ingest/bulk_ingest_es.py --es-url "http://localhost:9200" --index lol_matches --input "scripts/ingest/out/matches.jsonl" --ensure-index --batch-size 1000
```

## 3) If index exists but `docs.count=0`

Check in this order:

1. Collector output file size/line count is > 0
2. Each line has `matchId` and `side`
3. Bulk response has no failed items
4. `_id` is not accidentally constant
5. Writing target index is `lol_matches`

If you get `HTTP 403` with `error code: 1010`, first verify placeholders were replaced, then regenerate API key and retry without proxy/VPN.

Quick count check:

```powershell
Invoke-RestMethod "http://localhost:9200/lol_matches/_count"
```


