-- =============================================
-- LOLCAA 数据补丁：16.7.1（人工校准 v1）
-- 目标：
--   - 不改表结构（schema 不动）
--   - 尽量让数据更接近 16.7.1 的可用水平
--   - 避免重刷整库，仅用 UPDATE / 增量 UPSERT
-- 说明：
--   - 本文件刻意做成“小步可审查、可回滚”的增量脚本。
--   - 数值来源为“英雄定位 + 常见分路”的启发式估计，不是对局统计数据。
--   - 建议在测试库验证后再同步到正式库。
--
-- PowerShell（Windows）推荐执行方式：
--   Get-Content -Raw .\champion_patch_16_7_1_update.sql | docker exec -i lol-postgres psql -U postgres -d lol_db -v ON_ERROR_STOP=1
-- =============================================
-- Zed
UPDATE champion_stat_profile SET
                                 burst = 85, dps = 40, poke = 20,
                                 assassination = 95, mobility = 85, backline_access = 95,
                                 engage = 70, disengage = 60
WHERE champion_id = 238;

-- Talon
UPDATE champion_stat_profile SET
                                 burst = 80, dps = 35,
                                 assassination = 90, mobility = 90, backline_access = 92,
                                 engage = 75
WHERE champion_id = 91;

-- Qiyana
UPDATE champion_stat_profile SET
                                 burst = 90, dps = 35,
                                 assassination = 92, mobility = 80, backline_access = 88,
                                 engage = 75
WHERE champion_id = 246;

-- LeBlanc
UPDATE champion_stat_profile SET
                                 burst = 88, poke = 65,
                                 assassination = 90, mobility = 85, backline_access = 85,
                                 disengage = 80
WHERE champion_id = 7;

-- Fizz
UPDATE champion_stat_profile SET
                                 burst = 85,
                                 assassination = 90, mobility = 85, backline_access = 90,
                                 engage = 70, disengage = 80
WHERE champion_id = 105;

-- Ekko
UPDATE champion_stat_profile SET
                                 burst = 75, dps = 55,
                                 assassination = 80, mobility = 85, backline_access = 85,
                                 disengage = 90
WHERE champion_id = 245;

-- Katarina
UPDATE champion_stat_profile SET
                                 burst = 90, dps = 70,
                                 assassination = 95, mobility = 85, backline_access = 90
WHERE champion_id = 55;

-- Akali
UPDATE champion_stat_profile SET
                                 burst = 80, dps = 60,
                                 assassination = 90, mobility = 90, backline_access = 95,
                                 disengage = 80
WHERE champion_id = 84;
-- Kha'Zix
UPDATE champion_stat_profile SET
                                 burst = 85,
                                 assassination = 95, mobility = 80, backline_access = 90
WHERE champion_id = 121;

-- Rengar
UPDATE champion_stat_profile SET
                                 burst = 90,
                                 assassination = 98, mobility = 80, backline_access = 98
WHERE champion_id = 107;

-- Evelynn
UPDATE champion_stat_profile SET
                                 burst = 95,
                                 assassination = 98, mobility = 75, backline_access = 100
WHERE champion_id = 28;

-- Nocturne（半刺客）
UPDATE champion_stat_profile SET
                                 burst = 75, dps = 60,
                                 assassination = 80, backline_access = 95,
                                 engage = 90
WHERE champion_id = 56;

-- Kayn（刺客形态）
UPDATE champion_stat_profile SET
                                 burst = 85, dps = 65,
                                 assassination = 90, mobility = 85, backline_access = 90
WHERE champion_id = 141;
-- Yone（不是纯刺客，但能切后排）
UPDATE champion_stat_profile SET
                                 burst = 75, dps = 75,
                                 assassination = 70, mobility = 75, backline_access = 80
WHERE champion_id = 777;

-- Yasuo
UPDATE champion_stat_profile SET
                                 burst = 70, dps = 80,
                                 mobility = 80, backline_access = 75
WHERE champion_id = 157;

-- Irelia
UPDATE champion_stat_profile SET
                                 burst = 75, dps = 80,
                                 mobility = 80, backline_access = 80
WHERE champion_id = 39;

-- Diana（刺客玩法）
UPDATE champion_stat_profile SET
                                 burst = 85,
                                 assassination = 85, mobility = 75, backline_access = 85,
                                 engage = 80
WHERE champion_id = 131;

-- Sylas
UPDATE champion_stat_profile SET
                                 burst = 75, dps = 70,
                                 mobility = 70, backline_access = 75
WHERE champion_id = 517;
-- Pyke（支援刺客）
UPDATE champion_stat_profile SET
                                 burst = 70,
                                 assassination = 85, mobility = 85, backline_access = 85,
                                 engage = 80, disengage = 80
WHERE champion_id = 555;

-- Naafiri（直冲型）
UPDATE champion_stat_profile SET
                                 burst = 80,
                                 assassination = 88, mobility = 75, backline_access = 85
WHERE champion_id = 950;

-- Shaco
UPDATE champion_stat_profile SET
                                 burst = 85,
                                 assassination = 90, mobility = 80, backline_access = 90,
                                 disengage = 85
WHERE champion_id = 35;
-- ===== TOP 修正 =====
UPDATE champion SET primary_role = 'TOP', secondary_role = 'MID' WHERE id = 27;  -- Singed
UPDATE champion SET primary_role = 'TOP', secondary_role = 'MID' WHERE id = 82;  -- Mordekaiser
UPDATE champion SET primary_role = 'TOP', secondary_role = 'JUNGLE' WHERE id = 420; -- Illaoi（几乎纯上单）
UPDATE champion SET primary_role = 'TOP', secondary_role = 'MID' WHERE id = 39;  -- Irelia（主TOP副MID）

-- ===== MID 修正 =====
UPDATE champion SET primary_role = 'MID', secondary_role = 'SUP' WHERE id = 50;  -- Swain
UPDATE champion SET primary_role = 'MID', secondary_role = 'SUP' WHERE id = 63;  -- Brand
UPDATE champion SET primary_role = 'MID', secondary_role = 'SUP' WHERE id = 101; -- Xerath
UPDATE champion SET primary_role = 'MID', secondary_role = 'SUP' WHERE id = 161; -- Vel'Koz
UPDATE champion SET primary_role = 'MID', secondary_role = 'SUP' WHERE id = 134; -- Syndra（基本纯中）

-- ===== ADC 修正 =====
UPDATE champion SET primary_role = 'ADC', secondary_role = NULL WHERE id = 18;  -- Tristana（现在基本纯ADC）
UPDATE champion SET primary_role = 'ADC', secondary_role = NULL WHERE id = 22;  -- Ashe
UPDATE champion SET primary_role = 'ADC', secondary_role = NULL WHERE id = 51;  -- Caitlyn
UPDATE champion SET primary_role = 'ADC', secondary_role = NULL WHERE id = 222; -- Jinx
UPDATE champion SET primary_role = 'ADC', secondary_role = NULL WHERE id = 202; -- Jhin
UPDATE champion SET primary_role = 'ADC', secondary_role = NULL WHERE id = 145; -- Kai'Sa

-- ===== JUNGLE 修正 =====
UPDATE champion SET primary_role = 'JUNGLE', secondary_role = 'TOP' WHERE id = 2;  -- Olaf（现在主打野）
UPDATE champion SET primary_role = 'JUNGLE', secondary_role = 'TOP' WHERE id = 77; -- Udyr
UPDATE champion SET primary_role = 'JUNGLE', secondary_role = 'TOP' WHERE id = 72; -- Skarner
UPDATE champion SET primary_role = 'JUNGLE', secondary_role = 'TOP' WHERE id = 5;  -- Xin Zhao

-- ===== SUPPORT 修正 =====
UPDATE champion SET primary_role = 'SUP', secondary_role = NULL WHERE id = 16; -- Soraka
UPDATE champion SET primary_role = 'SUP', secondary_role = NULL WHERE id = 40; -- Janna
UPDATE champion SET primary_role = 'SUP', secondary_role = NULL WHERE id = 37; -- Sona
UPDATE champion SET primary_role = 'SUP', secondary_role = NULL WHERE id = 26; -- Zilean
UPDATE champion SET primary_role = 'SUP', secondary_role = NULL WHERE id = 43; -- Karma
-- ===== MID ↔ TOP =====
UPDATE champion SET primary_role = 'MID', secondary_role = 'TOP' WHERE id = 4;  -- Twisted Fate（几乎纯中）
UPDATE champion SET primary_role = 'MID', secondary_role = 'TOP' WHERE id = 90; -- Malzahar
UPDATE champion SET primary_role = 'MID', secondary_role = 'TOP' WHERE id = 13; -- Ryze

-- ===== ADC ↔ MID =====
UPDATE champion SET primary_role = 'ADC', secondary_role = 'MID' WHERE id = 42; -- Corki（偏ADC构建）
UPDATE champion SET primary_role = 'ADC', secondary_role = 'MID' WHERE id = 110; -- Varus

-- ===== SUPPORT ↔ MID =====
UPDATE champion SET primary_role = 'SUP', secondary_role = 'MID' WHERE id = 25; -- Morgana
UPDATE champion SET primary_role = 'SUP', secondary_role = 'MID' WHERE id = 117; -- Lulu
