param(
  [Parameter(Mandatory = $false)]
  [string]$ApiKey = $env:RIOT_API_KEY,

  [ValidateSet("asia", "europe", "americas", "sea")]
  [string]$Region = "asia",

  [int]$Start = 0,
  [int]$Count = 100,

  [string]$Puuid = "Dc7Da5YDuEd-3a9A0-ADQFWzDLfLYJXG07EOMk69ARZ6pehOV6Okbhb4zVvTTRdW4uQdo-k5dvidMQ",

  [string]$OutFile = "scripts/ingest/out/matches_100.jsonl"
)

if ([string]::IsNullOrWhiteSpace($ApiKey)) {
  throw "RIOT_API_KEY is empty. Pass -ApiKey or set `$env:RIOT_API_KEY first."
}

if ($Count -lt 1 -or $Count -gt 100) {
  throw "Count must be in range [1, 100]."
}

$outDir = Split-Path -Parent $OutFile
if (-not [string]::IsNullOrWhiteSpace($outDir)) {
  New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

$scriptPath = "scripts/ingest/collect_riot_matches.py"
if (-not (Test-Path $scriptPath)) {
  throw "Collector script not found: $scriptPath"
}

python $scriptPath --api-key $ApiKey --region $Region --puuid $Puuid --start $Start --count $Count --max-req-per-sec 20 --max-req-per-2min 100 --out $OutFile
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

Write-Host "Collection complete -> $OutFile"
