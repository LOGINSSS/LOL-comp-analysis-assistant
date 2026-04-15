package com.LOLCAA.service.impl;

import com.LOLCAA.domain.dto.LastPickAnalysisRequestDTO;
import com.LOLCAA.domain.po.*;
import com.LOLCAA.mapper.*;
import com.LOLCAA.service.LastPickAnalysisService;
import com.LOLCAA.utils.HeroParser;
import com.LOLCAA.utils.ScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 红色方最后一选分析服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LastPickAnalysisServiceImpl implements LastPickAnalysisService {

    private final ChampionMapper championMapper;
    private final ChampionMatchupMapper matchupMapper;
    private final ChampionSynergyMapper synergyMapper;
    private final ChampionTeamSynergyMapper teamSynergyMapper;
    private final ChampionArchetypeMapper archetypeMapper;
    private final ChampionStatProfileMapper statProfileMapper;
    private final DraftMapper draftMapper;
    private final DraftAnalysisMapper analysisMapper;

    private static final int L0_MIN_SAMPLES = 30;
    private static final int L1_MIN_SAMPLES = 50;
    private static final int L2_MIN_SAMPLES = 80;
    private static final int L3_MIN_SAMPLES = 50;
    private static final int L4_MIN_SAMPLES = 100;

    /** 小样本惩罚系数（越大越保守） */
    private static final double SAMPLE_PENALTY_LAMBDA = 6.0;

    @Override
    public DraftAnalysis analyzeLastPick(LastPickAnalysisRequestDTO request) {
        long startTime = System.currentTimeMillis();

        List<Long> bannedChampions = normalizeIds(request == null ? null : request.getBannedChampions());
        List<Long> allyPickedChampions = normalizeIds(request == null ? null : request.getAllyPickedChampions());
        List<Long> enemyPickedChampions = normalizeIds(request == null ? null : request.getEnemyPickedChampions());
        String requiredRole = normalizeRole(request == null ? null : request.getRequiredRole());

        Set<Long> unavailableChampions = new LinkedHashSet<>();
        unavailableChampions.addAll(bannedChampions);
        unavailableChampions.addAll(allyPickedChampions);
        unavailableChampions.addAll(enemyPickedChampions);

        List<Champion> availableChampions = championMapper.findAll().stream()
                .filter(Objects::nonNull)
                .filter(champion -> champion.getId() != null)
                .filter(champion -> !unavailableChampions.contains(champion.getId()))
                .filter(champion -> requiredRole == null || requiredRole.isBlank() || matchesRole(champion, requiredRole))
                .toList();

        if (availableChampions.isEmpty()) {
            throw new IllegalStateException("没有符合条件的可选英雄");
        }

        // ========== 退火/降级查询（当前用 PG 聚合表模拟统计层，后续换 ES 只需替换各 Level 的查询函数） ==========
        LevelContext ctx = buildLevelContext(requiredRole, allyPickedChampions, enemyPickedChampions);

        Map<Long, CandidateAnalysis> analysisByChampion = new HashMap<>();
        for (Champion champion : availableChampions) {
            CandidateAnalysis candidateAnalysis = analyzeCandidateWithFallbackLevels(
                    champion,
                    requiredRole,
                    allyPickedChampions,
                    enemyPickedChampions,
                    ctx
            );
            analysisByChampion.put(champion.getId(), candidateAnalysis);
        }

        CandidateAnalysis bestAnalysis = analysisByChampion.values().stream()
                .max(Comparator.comparingDouble(CandidateAnalysis::adjustedScore))
                .orElseThrow(() -> new IllegalStateException("无可用英雄"));

        Draft draft = createDraft(allyPickedChampions, enemyPickedChampions, bestAnalysis.champion().getId());
        draftMapper.insert(draft);

        DraftAnalysis analysis = createAnalysis(
                draft.getId(),
                bestAnalysis,
                allyPickedChampions,
                enemyPickedChampions,
                requiredRole,
                System.currentTimeMillis() - startTime
        );
        analysisMapper.insert(analysis);

        return analysis;
    }

    private CandidateAnalysis analyzeCandidateWithFallbackLevels(Champion candidate,
                                                                String requiredRole,
                                                                List<Long> allyPickedChampions,
                                                                List<Long> enemyPickedChampions,
                                                                LevelContext ctx) {
        // 说明：这里的“samples/games”是基于现有表里的 game_count 近似估计。
        // 若后续接入 ES：每个 Level 直接返回 {winRate, games} 即可。

        // Level 0：enemy5 + ally4（非常稀疏；这里在现有 PG 模型下无法严格表达，因此以“全对线覆盖度”作为近似）
        LevelResult l0 = level0_strict(candidate, requiredRole, enemyPickedChampions);
        if (l0.games() >= L0_MIN_SAMPLES) {
            return buildCandidateAnalysis(candidate, allyPickedChampions, enemyPickedChampions, l0);
        }

        // Level 1：enemy5 + ally2（核心位）固定
        LevelResult l1 = level1_enemy5_ally2(candidate, requiredRole, ctx.coreAllies(), enemyPickedChampions);
        if (l1.games() >= L1_MIN_SAMPLES) {
            return buildCandidateAnalysis(candidate, allyPickedChampions, enemyPickedChampions, l1);
        }

        // Level 2：enemy3（关键克制位） + ally2 固定
        LevelResult l2 = level2_enemy3_ally2(candidate, requiredRole, ctx.coreAllies(), ctx.keyEnemies());
        if (l2.games() >= L2_MIN_SAMPLES) {
            return buildCandidateAnalysis(candidate, allyPickedChampions, enemyPickedChampions, l2);
        }

        // Level 3：对位 + candidate 与 ally2 协同
        LevelResult l3 = level3_lane_plus_synergy(candidate, requiredRole, ctx.laneEnemy(), ctx.coreAllies());
        if (l3.games() >= L3_MIN_SAMPLES) {
            return buildCandidateAnalysis(candidate, allyPickedChampions, enemyPickedChampions, l3);
        }

        // Level 4：baseline（该位置强度，PG 中用 champion.winRate 近似；接 ES 后换为按 patch/rank/role 聚合）
        LevelResult l4 = level4_baseline(candidate);
        if (l4.games() >= L4_MIN_SAMPLES) {
            return buildCandidateAnalysis(candidate, allyPickedChampions, enemyPickedChampions, l4);
        }

        // 再兜底：完全无样本，按 50 中性返回
        return buildCandidateAnalysis(candidate, allyPickedChampions, enemyPickedChampions,
                new LevelResult(4, 50.0, 0, "fallback: no samples; use neutral 50"));
    }

    private CandidateAnalysis buildCandidateAnalysis(Champion candidate,
                                                    List<Long> allyPickedChampions,
                                                    List<Long> enemyPickedChampions,
                                                    LevelResult levelResult) {
        // 将“统计 winRate”映射为评分体系：matchupScore=winRate，其余维度先做简化保持兼容。
        // 这样前端仍然能展示分项；后续想保留更多解释，可在 detail 中附加 evidence。
        List<Long> fullAllies = new ArrayList<>(allyPickedChampions);
        fullAllies.add(candidate.getId());

        double matchupScore = ScoreCalculator.normalizeScore(levelResult.winRate());
        double synergyScore = calculateSynergyScore(fullAllies);
        double teamSynergyScore = calculateTeamSynergyScore(fullAllies);
        double allyDestructiveScore = calculateTeamThreatScore(fullAllies);
        double enemyDestructiveScore = calculateTeamThreatScore(enemyPickedChampions);
        double finalScore = ScoreCalculator.calculateFinalScore(matchupScore, synergyScore, teamSynergyScore, enemyDestructiveScore);

        double adjusted = adjustedScore(finalScore, levelResult.games());

        return new CandidateAnalysis(
                candidate,
                matchupScore,
                synergyScore,
                teamSynergyScore,
                allyDestructiveScore,
                enemyDestructiveScore,
                finalScore,
                adjusted,
                levelResult.levelUsed(),
                levelResult.games(),
                levelResult.reason()
        );
    }

    private double adjustedScore(double finalScore, int games) {
        if (games <= 0) {
            return finalScore - SAMPLE_PENALTY_LAMBDA;
        }
        return finalScore - SAMPLE_PENALTY_LAMBDA * Math.sqrt(1.0 / games);
    }

    /**
     * Level0 在当前 PG 聚合表模型中无法严格表达“enemy5 + ally4”组合胜率，先用“对线覆盖度”近似：
     * - 计算 candidate vs enemy 全体的对线均值（win_rate）
     * - games 取这些对线记录的 game_count 均值（近似）
     */
    private LevelResult level0_strict(Champion candidate,
                                     String requiredRole,
                                     List<Long> enemyPickedChampions) {
        LevelStat stat = matchupStatForCandidateVsEnemies(candidate.getId(), enemyPickedChampions, requiredRole);
        return new LevelResult(0, stat.winRate(), stat.games(),
                "L0 approx: lane matchup vs enemy5 (role=" + (requiredRole == null ? "" : requiredRole) + ") (PG model limitation)");
    }

    /** Level1：enemy5 + ally2 固定（PG 近似：candidate 对 enemy5 + candidate 与 ally2 协同） */
    private LevelResult level1_enemy5_ally2(Champion candidate,
                                          String requiredRole,
                                          List<Long> coreAllies,
                                          List<Long> enemyPickedChampions) {
        LevelStat lane = matchupStatForCandidateVsEnemies(candidate.getId(), enemyPickedChampions, requiredRole);
        LevelStat syn = synergyStatForCandidateWithAllies(candidate.getId(), coreAllies);

        double winRate = weightedAverage(lane.winRate(), lane.games(), syn.winRate(), syn.games(), 0.7);
        int games = Math.min(lane.games(), syn.games());
        return new LevelResult(1, winRate, games, "L1: enemy5 fixed + core allies synergy (approx via matchup+synergy)");
    }

    /** Level2：enemy3（关键位）+ ally2 固定（PG 近似：candidate vs keyEnemy3 + candidate 与 ally2 协同） */
    private LevelResult level2_enemy3_ally2(Champion candidate,
                                           String requiredRole,
                                           List<Long> coreAllies,
                                           List<Long> keyEnemies) {
        LevelStat lane = matchupStatForCandidateVsEnemies(candidate.getId(), keyEnemies, requiredRole);
        LevelStat syn = synergyStatForCandidateWithAllies(candidate.getId(), coreAllies);

        double winRate = weightedAverage(lane.winRate(), lane.games(), syn.winRate(), syn.games(), 0.75);
        int games = Math.min(lane.games(), syn.games());
        return new LevelResult(2, winRate, games, "L2: key enemy3 fixed + core allies synergy (approx via matchup+synergy)");
    }

    /** Level3：只看对位 + candidate 与 ally2 协同 */
    private LevelResult level3_lane_plus_synergy(Champion candidate,
                                                String requiredRole,
                                                Long laneEnemy,
                                                List<Long> coreAllies) {
        List<Long> enemies = laneEnemy == null ? List.of() : List.of(laneEnemy);
        LevelStat lane = matchupStatForCandidateVsEnemies(candidate.getId(), enemies, requiredRole);
        LevelStat syn = synergyStatForCandidateWithAllies(candidate.getId(), coreAllies);

        double winRate = weightedAverage(lane.winRate(), lane.games(), syn.winRate(), syn.games(), 0.6);
        int games = Math.min(lane.games(), syn.games());
        return new LevelResult(3, winRate, games, "L3: lane opponent + core allies synergy (approx)");
    }

    /** Level4：baseline 强度（PG 近似：champion.winRate + 假设样本量） */
    private LevelResult level4_baseline(Champion candidate) {
        Double winRate = candidate.getWinRate();
        double wr = winRate == null ? 50.0 : winRate;
        // PG 没有分 role/patch/rank 的 baseline sample，先给一个保守的虚拟样本量，保证排序更稳定。
        int games = winRate == null ? 0 : 200;
        return new LevelResult(4, wr, games, "L4: baseline champion.winRate (placeholder, replace by ES role/patch/rank baseline)");
    }

    private double weightedAverage(double a, int aN, double b, int bN, double aWeight) {
        if (aN <= 0 && bN <= 0) {
            return 50.0;
        }
        double aw = aN <= 0 ? 0.0 : aWeight;
        double bw = bN <= 0 ? 0.0 : (1.0 - aWeight);
        double wSum = aw + bw;
        if (wSum <= 0.0) {
            return 50.0;
        }
        return (a * aw + b * bw) / wSum;
    }

    private LevelStat matchupStatForCandidateVsEnemies(Long candidateId, List<Long> enemies, String requiredRole) {
        if (candidateId == null || enemies == null || enemies.isEmpty()) {
            return new LevelStat(50.0, 0);
        }
        // 复用现有表，但更贴近“candidate vs 指定敌人”的统计
        List<Double> winRates = matchupMapper.findWinRatesByRole(List.of(candidateId), enemies, requiredRole == null ? "" : requiredRole);
        double wr = ScoreCalculator.average(winRates);

        // games 取最小值近似（偏保守）；实际 ES 版本会返回真实 doc_count
        Integer games = findMinMatchupGameCount(candidateId, enemies, requiredRole);
        return new LevelStat(wr, games == null ? 0 : games);
    }

    private Integer findMinMatchupGameCount(Long candidateId, List<Long> enemies, String requiredRole) {
        // 为了最小改动，这里直接用 MyBatis-Plus 的 mapper 不存在现成方法。
        // 但 ChampionMatchup PO 有 gameCount 字段，后续可在 Mapper 增加查询。
        // 当前先返回 null：即游戏数未知（会触发降级或惩罚）。
        // 参数占位（后续接入真实 games 查询时会用到）
        //noinspection StatementWithEmptyBody
        if (candidateId == null || enemies == null || requiredRole == null) { /* placeholder */ }
        return null;
    }

    private LevelStat synergyStatForCandidateWithAllies(Long candidateId, List<Long> allies) {
        if (candidateId == null || allies == null || allies.isEmpty()) {
            return new LevelStat(50.0, 0);
        }

        List<Double> rates = new ArrayList<>();
        int games;
        boolean any = false;
        for (Long ally : allies) {
            if (ally == null) continue;
            Double r = synergyMapper.findPairWinRate(candidateId, ally);
            if (r != null) {
                rates.add(r);
                any = true;
            }
            // synergy 表也有 gameCount，但当前 mapper 没有返回；先不估。
        }

        double wr = any ? ScoreCalculator.average(rates) : 50.0;
        if (!any) {
            games = 0;
        } else {
            // 未能读取真实 games 时，给一个保守值，避免过度惩罚。
            games = 80;
        }
        return new LevelStat(wr, games);
    }

    /**
     * 根据 requiredRole 生成：核心 ally2、关键 enemy3、lane 对位敌人。
     * 说明：当前 request DTO 仅给了英雄列表，缺少“位置→英雄”的映射。
     * 这里用“按列表顺序假定为 TOP/JUNGLE/MID/ADC/SUP”进行近似：
     * - allyPickedChampions 视为从 TOP 开始顺序填充（不足则跳过）
     * - enemyPickedChampions 同理
     * 后续建议把 DTO 升级成位置化入参（Map<Role, championId>），即可消除该近似。
     */
    private LevelContext buildLevelContext(String requiredRole,
                                           List<Long> allyPickedChampions,
                                           List<Long> enemyPickedChampions) {
        Map<String, Long> allyByRole = toRoleMapAssumingOrder(allyPickedChampions);
        Map<String, Long> enemyByRole = toRoleMapAssumingOrder(enemyPickedChampions);

        List<String> coreRoles = coreAllyRoles(requiredRole);
        List<Long> coreAllies = coreRoles.stream()
                .map(allyByRole::get)
                .filter(Objects::nonNull)
                .toList();

        List<String> keyEnemyRoles = keyEnemyRoles(requiredRole);
        List<Long> keyEnemies = keyEnemyRoles.stream()
                .map(enemyByRole::get)
                .filter(Objects::nonNull)
                .toList();

        Long laneEnemy = enemyByRole.get(requiredRole);

        return new LevelContext(coreAllies, keyEnemies, laneEnemy);
    }

    private Map<String, Long> toRoleMapAssumingOrder(List<Long> picks) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (picks == null || picks.isEmpty()) {
            return map;
        }
        List<String> order = List.of("TOP", "JUNGLE", "MID", "ADC", "SUP");
        for (int i = 0; i < picks.size() && i < order.size(); i++) {
            Long id = picks.get(i);
            if (id != null) {
                map.put(order.get(i), id);
            }
        }
        return map;
    }

    /** 用户指定：缺 TOP 看 JG+MID；缺 JG 看 MID+SUP；缺 MID 看 JG+SUP；缺 ADC 看 SUP+JG；缺 SUP 看 ADC+JG */
    private List<String> coreAllyRoles(String requiredRole) {
        String role = requiredRole == null ? "" : requiredRole.toUpperCase(Locale.ROOT);
        return switch (role) {
            case "TOP" -> List.of("JUNGLE", "MID");
            case "JUNGLE" -> List.of("MID", "SUP");
            case "MID" -> List.of("JUNGLE", "SUP");
            case "ADC" -> List.of("SUP", "JUNGLE");
            case "SUP" -> List.of("ADC", "JUNGLE");
            default -> List.of("JUNGLE", "MID");
        };
    }

    /** 对敌人：在 coreRoles 基础上加对位（例如 TOP 看对面 TOP/MID/JUNGLE） */
    private List<String> keyEnemyRoles(String requiredRole) {
        String role = requiredRole == null ? "" : requiredRole.toUpperCase(Locale.ROOT);
        return switch (role) {
            case "TOP" -> List.of("TOP", "MID", "JUNGLE");
            case "JUNGLE" -> List.of("JUNGLE", "MID", "SUP");
            case "MID" -> List.of("MID", "JUNGLE", "SUP");
            case "ADC" -> List.of("ADC", "SUP", "JUNGLE");
            case "SUP" -> List.of("SUP", "ADC", "JUNGLE");
            default -> List.of("MID", "JUNGLE", "SUP");
        };
    }

    private double calculateSynergyScore(List<Long> allies) {
        if (allies.size() < 2) {
            return 50.0;
        }
        List<Double> synergyRates = synergyMapper.findSynergyWinRates(allies);
        return ScoreCalculator.average(synergyRates);
    }

    private double calculateTeamSynergyScore(List<Long> allies) {
        if (allies.isEmpty()) {
            return 50.0;
        }
        String championIdsStr = HeroParser.formatHeroIdsForSynergy(allies);
        Double score = teamSynergyMapper.findTeamSynergyScore(championIdsStr);
        return score != null ? score : 50.0;
    }

    private double calculateTeamThreatScore(List<Long> teamChampions) {
        if (teamChampions.isEmpty()) {
            return 0.0;
        }

        double rawThreat = 0.0;
        for (Long championId : teamChampions) {
            ChampionStatProfile profile = statProfileMapper.findByChampionId(championId);
            ChampionArchetype archetype = archetypeMapper.findByChampionId(championId);

            if (profile != null) {
                rawThreat += safeValue(profile.getCc()) * 1.5;
                rawThreat += safeValue(profile.getEngage());
                rawThreat += safeValue(profile.getBurst()) * 1.2;
                rawThreat += safeValue(profile.getBacklineAccess());
                rawThreat += safeValue(profile.getMobility()) * 0.5;
                rawThreat += safeValue(profile.getFrontline()) * 0.8;
            }

            if (archetype != null && "TANK".equalsIgnoreCase(archetype.getSubRole())) {
                rawThreat += 4.0;
            }
            if (archetype != null && "ENGAGE".equalsIgnoreCase(archetype.getSubRole())) {
                rawThreat += 4.0;
            }
        }

        return ScoreCalculator.normalizeScore((rawThreat / teamChampions.size()) * 8.0);
    }

    private DraftAnalysis createAnalysis(Long draftId,
                                        CandidateAnalysis bestAnalysis,
                                        List<Long> allyPickedChampions,
                                        List<Long> enemyPickedChampions,
                                        String requiredRole,
                                        long analysisTime) {
        List<Long> fullAllies = new ArrayList<>(allyPickedChampions);
        fullAllies.add(bestAnalysis.champion().getId());

        DraftAnalysis analysis = new DraftAnalysis();
        analysis.setDraftId(draftId);
        analysis.setMatchupScore(bestAnalysis.matchupScore());
        analysis.setMatchupDetail(buildMatchupDetail(bestAnalysis, allyPickedChampions, enemyPickedChampions, requiredRole));
        analysis.setSynergyScore(bestAnalysis.synergyScore());
        analysis.setSynergyDetail(buildSynergyDetail(bestAnalysis, fullAllies, requiredRole));
        analysis.setTeamSynergyScore(bestAnalysis.teamSynergyScore());
        analysis.setTeamSynergyDetail(buildTeamSynergyDetail(bestAnalysis.champion(), bestAnalysis.teamSynergyScore(), fullAllies));
        analysis.setAllyDestructiveScore(bestAnalysis.allyDestructiveScore());
        analysis.setEnemyDestructiveScore(bestAnalysis.enemyDestructiveScore());
        analysis.setFinalScore(bestAnalysis.finalScore());
        analysis.setRecommendation(buildRecommendation(bestAnalysis, requiredRole));
        analysis.setWinProbability(ScoreCalculator.generateWinProbability(bestAnalysis.finalScore()));
        analysis.setCreateTime(System.currentTimeMillis());
        analysis.setAnalysisTime(analysisTime);

        return analysis;
    }

    private Draft createDraft(List<Long> allies, List<Long> enemies, Long newAlly) {
        List<Long> fullAllies = new ArrayList<>(allies);
        fullAllies.add(newAlly);

        Draft draft = new Draft();
        draft.setAllyTeamIds(HeroParser.formatHeroIdsForSynergy(fullAllies));
        draft.setEnemyTeamIds(HeroParser.formatHeroIdsForSynergy(enemies));
        draft.setCreateTime(System.currentTimeMillis());
        draft.setRegion("demo");
        return draft;
    }

    private String buildMatchupDetail(CandidateAnalysis analysis,
                                      List<Long> allyPickedChampions,
                                      List<Long> enemyPickedChampions,
                                      String requiredRole) {
        return String.format(Locale.ROOT,
                "{\"dimension\":\"matchup\",\"levelUsed\":%d,\"games\":%d,\"fallbackReason\":\"%s\",\"championId\":%d,\"championName\":\"%s\",\"requiredRole\":\"%s\",\"score\":%.1f,\"allies\":\"%s\",\"enemies\":\"%s\"}",
                analysis.levelUsed(),
                analysis.games(),
                safeText(analysis.fallbackReason()),
                analysis.champion().getId(),
                safeText(analysis.champion().getName()),
                safeText(requiredRole),
                analysis.matchupScore(),
                safeText(allyPickedChampions.toString()),
                safeText(enemyPickedChampions.toString()));
    }

    private String buildSynergyDetail(CandidateAnalysis analysis,
                                      List<Long> fullAllies,
                                      String requiredRole) {
        return String.format(Locale.ROOT,
                "{\"dimension\":\"synergy\",\"levelUsed\":%d,\"games\":%d,\"fallbackReason\":\"%s\",\"championId\":%d,\"championName\":\"%s\",\"requiredRole\":\"%s\",\"score\":%.1f,\"allies\":\"%s\"}",
                analysis.levelUsed(),
                analysis.games(),
                safeText(analysis.fallbackReason()),
                analysis.champion().getId(),
                safeText(analysis.champion().getName()),
                safeText(requiredRole),
                analysis.synergyScore(),
                safeText(fullAllies.toString()));
    }

    private String buildTeamSynergyDetail(Champion champion,
                                          double teamSynergyScore,
                                          List<Long> fullAllies) {
        return String.format(Locale.ROOT,
                "{\"dimension\":\"team_synergy\",\"championId\":%d,\"championName\":\"%s\",\"score\":%.1f,\"team\":\"%s\"}",
                champion.getId(),
                safeText(champion.getName()),
                teamSynergyScore,
                safeText(HeroParser.formatHeroIdsForSynergy(fullAllies)));
    }

    private String buildRecommendation(CandidateAnalysis analysis, String requiredRole) {
        return String.format(Locale.ROOT,
                "推荐最后一选：%s(%d)，需求位置=%s，level=%d，样本=%d，综合评分=%.1f，校正评分=%.2f",
                safeText(analysis.champion().getName()),
                analysis.champion().getId(),
                safeText(requiredRole),
                analysis.levelUsed(),
                analysis.games(),
                analysis.finalScore(),
                analysis.adjustedScore());
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(ids);
    }

    private String normalizeRole(String role) {
        return role == null ? null : role.trim().toUpperCase(Locale.ROOT);
    }

    private boolean matchesRole(Champion champion, String requiredRole) {
        return requiredRole.equalsIgnoreCase(Optional.ofNullable(champion.getPrimaryRole()).orElse(""))
                || requiredRole.equalsIgnoreCase(Optional.ofNullable(champion.getSecondaryRole()).orElse(""));
    }

    private double safeValue(Integer value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private String safeText(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }

    private record LevelContext(List<Long> coreAllies, List<Long> keyEnemies, Long laneEnemy) {
    }

    private record LevelResult(int levelUsed, double winRate, int games, String reason) {
    }

    private record LevelStat(double winRate, int games) {
    }

    private static final class CandidateAnalysis {
        private final Champion champion;
        private final double matchupScore;
        private final double synergyScore;
        private final double teamSynergyScore;
        private final double allyDestructiveScore;
        private final double enemyDestructiveScore;
        private final double finalScore;

        /** 统一重排后的评分（finalScore - f(samples)） */
        private final double adjustedScore;

        /** 命中的降级层级：0~4 */
        private final int levelUsed;

        /** 样本量（近似/或 ES doc_count） */
        private final int games;

        /** 降级原因/证据（用于解释） */
        private final String fallbackReason;

        private CandidateAnalysis(Champion champion,
                                  double matchupScore,
                                  double synergyScore,
                                  double teamSynergyScore,
                                  double allyDestructiveScore,
                                  double enemyDestructiveScore,
                                  double finalScore,
                                  double adjustedScore,
                                  int levelUsed,
                                  int games,
                                  String fallbackReason) {
            this.champion = champion;
            this.matchupScore = matchupScore;
            this.synergyScore = synergyScore;
            this.teamSynergyScore = teamSynergyScore;
            this.allyDestructiveScore = allyDestructiveScore;
            this.enemyDestructiveScore = enemyDestructiveScore;
            this.finalScore = finalScore;
            this.adjustedScore = adjustedScore;
            this.levelUsed = levelUsed;
            this.games = games;
            this.fallbackReason = fallbackReason;
        }

        private Champion champion() {
            return champion;
        }

        private double matchupScore() {
            return matchupScore;
        }

        private double synergyScore() {
            return synergyScore;
        }

        private double teamSynergyScore() {
            return teamSynergyScore;
        }

        private double allyDestructiveScore() {
            return allyDestructiveScore;
        }

        private double enemyDestructiveScore() {
            return enemyDestructiveScore;
        }

        private double finalScore() {
            return finalScore;
        }

        private double adjustedScore() {
            return adjustedScore;
        }

        private int levelUsed() {
            return levelUsed;
        }

        private int games() {
            return games;
        }

        private String fallbackReason() {
            return fallbackReason == null ? "" : fallbackReason;
        }
    }
}

