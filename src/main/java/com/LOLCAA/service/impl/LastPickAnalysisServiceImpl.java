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

        Map<Long, CandidateAnalysis> analysisByChampion = new HashMap<>();
        for (Champion champion : availableChampions) {
            List<Long> fullAllies = new ArrayList<>(allyPickedChampions);
            fullAllies.add(champion.getId());

            double matchupScore = calculateMatchupScore(fullAllies, enemyPickedChampions);
            double synergyScore = calculateSynergyScore(fullAllies);
            double teamSynergyScore = calculateTeamSynergyScore(fullAllies);
            double allyDestructiveScore = calculateTeamThreatScore(fullAllies);
            double enemyDestructiveScore = calculateTeamThreatScore(enemyPickedChampions);
            double totalScore = ScoreCalculator.calculateFinalScore(matchupScore, synergyScore, teamSynergyScore, enemyDestructiveScore);

            analysisByChampion.put(champion.getId(), new CandidateAnalysis(
                    champion,
                    matchupScore,
                    synergyScore,
                    teamSynergyScore,
                    allyDestructiveScore,
                    enemyDestructiveScore,
                    totalScore
            ));
        }

        CandidateAnalysis bestAnalysis = analysisByChampion.values().stream()
                .max(Comparator.comparingDouble(CandidateAnalysis::finalScore))
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

    private double calculateMatchupScore(List<Long> allies, List<Long> enemies) {
        if (allies.isEmpty() || enemies.isEmpty()) {
            return 50.0;
        }
        List<Double> winRates = matchupMapper.findWinRates(allies, enemies);
        return ScoreCalculator.average(winRates);
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
        analysis.setMatchupDetail(buildMatchupDetail(bestAnalysis.champion(), bestAnalysis.matchupScore(), allyPickedChampions, enemyPickedChampions, requiredRole));
        analysis.setSynergyScore(bestAnalysis.synergyScore());
        analysis.setSynergyDetail(buildSynergyDetail(bestAnalysis.champion(), bestAnalysis.synergyScore(), fullAllies, requiredRole));
        analysis.setTeamSynergyScore(bestAnalysis.teamSynergyScore());
        analysis.setTeamSynergyDetail(buildTeamSynergyDetail(bestAnalysis.champion(), bestAnalysis.teamSynergyScore(), fullAllies));
        analysis.setAllyDestructiveScore(bestAnalysis.allyDestructiveScore());
        analysis.setEnemyDestructiveScore(bestAnalysis.enemyDestructiveScore());
        analysis.setFinalScore(bestAnalysis.finalScore());
        analysis.setRecommendation(buildRecommendation(bestAnalysis.champion(), requiredRole, bestAnalysis.finalScore()));
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

    private String buildMatchupDetail(Champion champion,
                                      double matchupScore,
                                      List<Long> allyPickedChampions,
                                      List<Long> enemyPickedChampions,
                                      String requiredRole) {
        return String.format(Locale.ROOT,
                "{\"dimension\":\"matchup\",\"championId\":%d,\"championName\":\"%s\",\"requiredRole\":\"%s\",\"score\":%.1f,\"allies\":\"%s\",\"enemies\":\"%s\"}",
                champion.getId(),
                safeText(champion.getName()),
                safeText(requiredRole),
                matchupScore,
                safeText(allyPickedChampions.toString()),
                safeText(enemyPickedChampions.toString()));
    }

    private String buildSynergyDetail(Champion champion,
                                      double synergyScore,
                                      List<Long> fullAllies,
                                      String requiredRole) {
        return String.format(Locale.ROOT,
                "{\"dimension\":\"synergy\",\"championId\":%d,\"championName\":\"%s\",\"requiredRole\":\"%s\",\"score\":%.1f,\"allies\":\"%s\"}",
                champion.getId(),
                safeText(champion.getName()),
                safeText(requiredRole),
                synergyScore,
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

    private String buildRecommendation(Champion champion, String requiredRole, double finalScore) {
        return String.format(Locale.ROOT,
                "推荐最后一选：%s(%d)，需求位置=%s，综合评分=%.1f",
                safeText(champion.getName()),
                champion.getId(),
                safeText(requiredRole),
                finalScore);
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

    private static final class CandidateAnalysis {
        private final Champion champion;
        private final double matchupScore;
        private final double synergyScore;
        private final double teamSynergyScore;
        private final double allyDestructiveScore;
        private final double enemyDestructiveScore;
        private final double finalScore;

        private CandidateAnalysis(Champion champion,
                                  double matchupScore,
                                  double synergyScore,
                                  double teamSynergyScore,
                                  double allyDestructiveScore,
                                  double enemyDestructiveScore,
                                  double finalScore) {
            this.champion = champion;
            this.matchupScore = matchupScore;
            this.synergyScore = synergyScore;
            this.teamSynergyScore = teamSynergyScore;
            this.allyDestructiveScore = allyDestructiveScore;
            this.enemyDestructiveScore = enemyDestructiveScore;
            this.finalScore = finalScore;
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
    }
}

