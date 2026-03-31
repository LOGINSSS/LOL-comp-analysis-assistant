-- =========================
-- Draft Analysis 选秀分析
-- =========================
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