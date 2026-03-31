-- Champion 表
CREATE TABLE champion (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(100) UNIQUE NOT NULL,
                          role VARCHAR(20),
                          tier VARCHAR(10),
                          pick_rate DECIMAL(5,2),
                          win_rate DECIMAL(5,2)
);

-- ChampionMatchup 表
CREATE TABLE champion_matchup (
                                  id BIGSERIAL PRIMARY KEY,
                                  champion_id BIGINT NOT NULL,
                                  vs_champion_id BIGINT NOT NULL,
                                  win_rate DECIMAL(5,2),
                                  game_count INT,
                                  role VARCHAR(20),
                                  last_update_time BIGINT,
                                  FOREIGN KEY (champion_id) REFERENCES champion(id),
                                  FOREIGN KEY (vs_champion_id) REFERENCES champion(id)
);

-- ChampionSynergy 表
CREATE TABLE champion_synergy (
                                  id BIGSERIAL PRIMARY KEY,
                                  champion1_id BIGINT NOT NULL,
                                  champion2_id BIGINT NOT NULL,
                                  win_rate DECIMAL(5,2),
                                  game_count INT,
                                  source VARCHAR(20),
                                  last_update_time BIGINT,
                                  FOREIGN KEY (champion1_id) REFERENCES champion(id),
                                  FOREIGN KEY (champion2_id) REFERENCES champion(id)
);

-- ChampionTeamSynergy 表
CREATE TABLE champion_team_synergy (
                                       id BIGSERIAL PRIMARY KEY,
                                       champion_ids VARCHAR(500) NOT NULL,
                                       team_synergy_score DECIMAL(5,2),
                                       training_data_count INT,
                                       description TEXT,
                                       last_update_time BIGINT
);

-- Draft 表
CREATE TABLE draft (
                       id BIGSERIAL PRIMARY KEY,
                       ally_team_ids VARCHAR(100) NOT NULL,
                       enemy_team_ids VARCHAR(100) NOT NULL,
                       create_time BIGINT,
                       region VARCHAR(50)
);

-- DraftAnalysis 表
CREATE TABLE draft_analysis (
                                id BIGSERIAL PRIMARY KEY,
                                draft_id BIGINT NOT NULL UNIQUE,
                                matchup_score DECIMAL(5,2),
                                matchup_detail TEXT,
                                synergy_score DECIMAL(5,2),
                                synergy_detail TEXT,
                                team_synergy_score DECIMAL(5,2),
                                team_synergy_detail TEXT,
                                ally_destructive_score DECIMAL(5,2),
                                enemy_destructive_score DECIMAL(5,2),
                                final_score DECIMAL(5,2),
                                recommendation VARCHAR(200),
                                win_probability VARCHAR(20),
                                create_time BIGINT,
                                analysis_time BIGINT,
                                FOREIGN KEY (draft_id) REFERENCES draft(id)
);