-- =========================
-- Champion（核心实体）
-- =========================
CREATE TABLE champion (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(100) UNIQUE NOT NULL,

    -- 主位置体系（稳定）
                          primary_role VARCHAR(20),   -- TOP / JG / MID / ADC / SUP
                          secondary_role VARCHAR(20),

                          tier VARCHAR(10),
                          pick_rate DECIMAL(5,2),
                          win_rate DECIMAL(5,2)
);

-- =========================
-- Draft（对局结构）
-- =========================
CREATE TABLE draft (
                       id BIGSERIAL PRIMARY KEY,

                       ally_team_ids VARCHAR(100) NOT NULL,
                       enemy_team_ids VARCHAR(100) NOT NULL,

                       create_time BIGINT,
                       region VARCHAR(50)
);