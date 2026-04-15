param(
  [Parameter(Mandatory = $true)]
  [string]$ApiKey,

  [Parameter(Mandatory = $false)]
  [string]$RiotId,

  [Parameter(Mandatory = $false)]
  [string]$GameName,

  [Parameter(Mandatory = $false)]
  [string]$TagLine,

  [ValidateSet("asia", "americas", "europe", "sea")]
  [string]$Region = "asia",

  [switch]$SetEnv,
  [switch]$Json
)

function Test-Placeholder([string]$Value) {
  if ([string]::IsNullOrWhiteSpace($Value)) {
    return $false
  }
  $trimmed = $Value.Trim()
  return $trimmed.StartsWith("<") -and $trimmed.EndsWith(">")
}

function Resolve-RiotIdInput {
  param(
    [string]$InputRiotId,
    [string]$InputGameName,
    [string]$InputTagLine
  )

  if (-not [string]::IsNullOrWhiteSpace($InputRiotId)) {
    $parts = $InputRiotId.Split("#", 2)
    if ($parts.Count -ne 2 -or [string]::IsNullOrWhiteSpace($parts[0]) -or [string]::IsNullOrWhiteSpace($parts[1])) {
      throw "RiotId format is invalid. Expected: gameName#tagLine"
    }
    return @{ GameName = $parts[0]; TagLine = $parts[1] }
  }

  if ([string]::IsNullOrWhiteSpace($InputGameName) -or [string]::IsNullOrWhiteSpace($InputTagLine)) {
    throw "Provide either -RiotId 'gameName#tagLine' or both -GameName and -TagLine."
  }

  $resolvedTag = $InputTagLine.TrimStart("#")
  return @{ GameName = $InputGameName; TagLine = $resolvedTag }
}

try {
  if (Test-Placeholder $ApiKey) {
    throw "ApiKey still looks like a placeholder. Replace <RIOT_API_KEY> with your real key."
  }

  $resolved = Resolve-RiotIdInput -InputRiotId $RiotId -InputGameName $GameName -InputTagLine $TagLine

  if (Test-Placeholder $resolved.GameName -or Test-Placeholder $resolved.TagLine) {
    throw "Riot ID still looks like a placeholder. Replace with real values."
  }

  $encodedGameName = [System.Uri]::EscapeDataString($resolved.GameName)
  $encodedTagLine = [System.Uri]::EscapeDataString($resolved.TagLine)
  $url = "https://$Region.api.riotgames.com/riot/account/v1/accounts/by-riot-id/$encodedGameName/$encodedTagLine"

  $headers = @{ "X-Riot-Token" = $ApiKey }
  $resp = Invoke-RestMethod -Method GET -Uri $url -Headers $headers

  if (-not $resp.puuid) {
    throw "Response does not contain puuid."
  }

  if ($Json) {
    [PSCustomObject]@{
      gameName = $resp.gameName
      tagLine = $resp.tagLine
      puuid = $resp.puuid
      region = $Region
    } | ConvertTo-Json -Depth 3
  }
  else {
    Write-Host "gameName: $($resp.gameName)"
    Write-Host "tagLine : $($resp.tagLine)"
    Write-Host "puuid   : $($resp.puuid)"
  }

  if ($SetEnv) {
    $env:RIOT_PUUID = $resp.puuid
    Write-Host "RIOT_PUUID has been set for current shell session."
  }
}
catch {
  $msg = $_.Exception.Message
  Write-Error "Request failed: $msg"

  if ($msg -match "403") {
    Write-Host "Hint: API key expired/invalid, region mismatch, or request blocked."
  }
  elseif ($msg -match "404") {
    Write-Host "Hint: Riot ID not found. Check gameName and tagLine."
  }
  elseif ($msg -match "429") {
    Write-Host "Hint: Rate limit hit, wait and retry."
  }

  exit 1
}

