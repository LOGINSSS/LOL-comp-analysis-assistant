-- Java ingest module runtime tables

CREATE TABLE IF NOT EXISTS processed_match (
    match_id VARCHAR(32) PRIMARY KEY,
    processed_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ingest_progress (
    puuid VARCHAR(100) PRIMARY KEY,
    last_match_end_ts BIGINT,
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS summoner_pool (
    puuid VARCHAR(100) PRIMARY KEY,
    summoner_name VARCHAR(100),
    tier VARCHAR(20),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_summoner_pool_updated_at ON summoner_pool(updated_at DESC);

