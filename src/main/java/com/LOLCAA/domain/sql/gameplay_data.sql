-- =========================
-- Champion Matchup 对线数据
-- =========================
CREATE TABLE champion_matchup (
                                  id BIGSERIAL PRIMARY KEY,

                                  champion_id BIGINT NOT NULL,
                                  vs_champion_id BIGINT NOT NULL,

                                  win_rate DECIMAL(5,2),
                                  game_count INT,

                                  role VARCHAR(20), -- 为未来扩展预留
                                  last_update_time BIGINT,

                                  FOREIGN KEY (champion_id) REFERENCES champion(id),
                                  FOREIGN KEY (vs_champion_id) REFERENCES champion(id)
);

-- =========================
-- Champion Synergy 英雄协同数据
-- =========================
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

-- =========================
-- Team Synergy 团队协同数据（基于整体阵容的分析结果，非单纯的英雄组合）
-- =========================
CREATE TABLE champion_team_synergy (
                                       id BIGSERIAL PRIMARY KEY,

                                       champion_ids VARCHAR(500) NOT NULL,

                                       team_synergy_score DECIMAL(5,2),
                                       training_data_count INT,
                                       description TEXT,

                                       last_update_time BIGINT
);