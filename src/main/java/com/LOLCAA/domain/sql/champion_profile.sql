-- =========================
-- Champion Archetype（宏观分类）
-- =========================
CREATE TABLE champion_archetype (
                                    champion_id BIGINT PRIMARY KEY,

                                    macro_role VARCHAR(20),  -- FRONTLINE / BACKLINE / ASSASSIN
                                    sub_role VARCHAR(30),    -- TANK / DPS / POKE / ENGAGE 等

                                    FOREIGN KEY (champion_id) REFERENCES champion(id)
);

-- =========================
-- Champion Stat Profile（能力评分核心表）
-- =========================
CREATE TABLE champion_stat_profile (
                                       champion_id BIGINT PRIMARY KEY,

    -- 🛡️ 生存 / 前排
                                       tankiness INT DEFAULT 0,
                                       frontline INT DEFAULT 0,

    -- ⚔️ 输出
                                       burst INT DEFAULT 0,
                                       dps INT DEFAULT 0,
                                       poke INT DEFAULT 0,

    -- 🎯 控制 / 功能
                                       cc INT DEFAULT 0,
                                       engage INT DEFAULT 0,
                                       disengage INT DEFAULT 0,
                                       peel INT DEFAULT 0,

    -- 💊 辅助能力
                                       heal INT DEFAULT 0,
                                       shield INT DEFAULT 0,
                                       buff INT DEFAULT 0,
                                       debuff INT DEFAULT 0,

    -- 🗡️ 刺客能力
                                       assassination INT DEFAULT 0,
                                       mobility INT DEFAULT 0,
                                       backline_access INT DEFAULT 0,

                                       FOREIGN KEY (champion_id) REFERENCES champion(id)
);