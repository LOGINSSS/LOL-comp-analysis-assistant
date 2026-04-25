package com.LOLCAA.service.impl;

import com.LOLCAA.domain.dto.LastPickAnalysisRequestDTO;
import com.LOLCAA.domain.dto.LastPickRecommendationDTO;
import com.LOLCAA.domain.po.Champion;
import com.LOLCAA.domain.po.Draft;
import com.LOLCAA.domain.po.DraftAnalysis;
import com.LOLCAA.mapper.ChampionMapper;
import com.LOLCAA.mapper.DraftAnalysisMapper;
import com.LOLCAA.mapper.DraftMapper;
import com.LOLCAA.service.LastPickAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 最后一选分析实现（ES 聚合版本）。
 *
 * 核心思路：
 * 1) 以 requiredRole 作为候选聚合字段；
 * 2) 按 P0~P5 逐层放宽过滤条件（退火） ;
 * 3) 命中后输出 TopN，若全层级失败则回退英雄表兜底；
 * 4) 将最佳结果写入 draft / draft_analysis 便于回看。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LastPickAnalysisServiceImpl implements LastPickAnalysisService {

    private static final int ES_AGG_SIZE = 5;
    private static final int ES_MIN_DOC_COUNT = 1;
    private static final int CANDIDATE_MIN_GAMES = 1;
    private static final int ES_SHARD_SIZE = (int) Math.ceil(ES_AGG_SIZE * 1.5 + 10);

    private static final Map<String, List<String>> ALLY_CORE_BY_ROLE = Map.of(
            "TOP", List.of("MID", "JUNGLE"),
            "MID", List.of("JUNGLE", "SUP"),
            "JUNGLE", List.of("MID", "SUP"),
            "ADC", List.of("JUNGLE", "SUP"),
            "SUP", List.of("ADC", "JUNGLE")
    );

    private static final Map<String, List<String>> ENEMY_CORE_BY_ROLE = Map.of(
            "TOP", List.of("TOP", "JUNGLE", "MID"),
            "MID", List.of("MID", "JUNGLE", "SUP"),
            "JUNGLE", List.of("JUNGLE", "MID", "SUP"),
            "ADC", List.of("ADC", "SUP", "JUNGLE"),
            "SUP", List.of("SUP", "ADC", "JUNGLE")
    );

    private static final Map<String, String> ALLY_FIELD_BY_ROLE = Map.of(
            "TOP", "top",
            "JUNGLE", "jungle",
            "MID", "mid",
            "ADC", "adc",
            "SUP", "sup"
    );

    private static final Map<String, String> ENEMY_FIELD_BY_ROLE = Map.of(
            "TOP", "enemyTop",
            "JUNGLE", "enemyJungle",
            "MID", "enemyMid",
            "ADC", "enemyAdc",
            "SUP", "enemySup"
    );

    /**
     * 退火阶段定义：从严格到宽松。
     */
    private static final List<StageSpec> STAGES = List.of(
            new StageSpec("P0", 1, 0.95),
            new StageSpec("P1", 1, 0.85),
            new StageSpec("P2", 1, 0.78),
            new StageSpec("P3", 1, 0.68),
            new StageSpec("P4", 1, 0.55),
            new StageSpec("P5", 1, 0.40)
    );

    private final ChampionMapper championMapper;
    private final DraftMapper draftMapper;
    private final DraftAnalysisMapper analysisMapper;
    private final ObjectMapper objectMapper;

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUris;

    @Value("${riot.ingest.index-name:lol_matches}")
    private String indexName;

    private volatile WebClient webClient;

    /**
     * 分析入口：执行退火查询并返回 TopN 推荐。
     */
    @Override
    public List<LastPickRecommendationDTO> analyzeLastPick(LastPickAnalysisRequestDTO request) {
        long start = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        String requiredRole = normalizeRole(request == null ? null : request.getRequiredRole());
        if (requiredRole == null) {
            throw new IllegalArgumentException("requiredRole is required: TOP/JUNGLE/MID/ADC/SUP");
        }

        int topN = normalizeTopN(request == null ? null : request.getTopN());
        Map<String, Long> allyByRole = allyByRole(request);
        Map<String, Long> enemyByRole = enemyByRole(request);
        Set<Long> unavailable = unavailableChampions(request, allyByRole, enemyByRole);

        List<Champion> champions = championMapper.findAll();
        Map<Long, String> championNames = champions.stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Champion::getId, Champion::getName, (a, b) -> a));

        List<String> fallbackReasons = new ArrayList<>();
        List<String> stageDiagnostics = new ArrayList<>();
        List<LastPickRecommendationDTO> finalRecommendations = Collections.emptyList();
        String selectedStage = "P5";

        for (StageSpec stage : STAGES) {
            Map<String, String> fixed = fixedFiltersForStage(stage.level(), requiredRole, allyByRole, enemyByRole);
            QueryResult queryResult = queryCandidates(requiredRole, fixed, unavailable);

            List<CandidateStat> eligibleItems = queryResult.items().stream()
                    .filter(it -> it.games() >= CANDIDATE_MIN_GAMES)
                    .toList();

            String diagnosticPrefix = stage.level() + "{filters=" + fixed
                    + ",totalGames=" + queryResult.totalGames()
                    + ",buckets=" + queryResult.items().size()
                    + ",eligible=" + eligibleItems.size() + "}";

            if (eligibleItems.isEmpty()) {
                if (queryResult.totalGames() > 0) {
                    fallbackReasons.add(stage.level() + ": no candidate buckets (below min sample threshold)");
                    stageDiagnostics.add(diagnosticPrefix + " => reject:no_candidate_buckets_below_threshold");
                } else {
                    fallbackReasons.add(stage.level() + ": no candidate buckets");
                    stageDiagnostics.add(diagnosticPrefix + " => reject:no_candidate_buckets");
                }
                continue;
            }

            if (stage.minSamples() != null && queryResult.totalGames() < stage.minSamples()) {
                fallbackReasons.add(stage.level() + ": samples " + queryResult.totalGames() + " < " + stage.minSamples());
                stageDiagnostics.add(diagnosticPrefix + " => reject:insufficient_samples");
                continue;
            }

            CandidateStat topCandidate = eligibleItems.stream()
                    .max(Comparator.comparingDouble(CandidateStat::winRate))
                    .orElse(null);
            if (topCandidate != null) {
                stageDiagnostics.add(diagnosticPrefix + " => accept:top=" + topCandidate.championId()
                        + ",wr=" + round2(topCandidate.winRate())
                        + ",games=" + topCandidate.games());
            } else {
                stageDiagnostics.add(diagnosticPrefix + " => accept");
            }

            selectedStage = stage.level();
            finalRecommendations = toRecommendations(
                    eligibleItems,
                    championNames,
                    stage,
                    topN,
                    evidence(requiredRole, fixed),
                    String.join("; ", fallbackReasons)
            );
            break;
        }

        if (finalRecommendations.isEmpty()) {
            finalRecommendations = fallbackFromChampionTable(champions, requiredRole, unavailable, topN, String.join("; ", fallbackReasons));
            selectedStage = "P5";
            stageDiagnostics.add("fallback:champion_table");
        }

        long elapsedMs = System.currentTimeMillis() - start;
        logHitDiagnostics(traceId, requiredRole, selectedStage, allyByRole, enemyByRole, unavailable,
                finalRecommendations, fallbackReasons, stageDiagnostics, elapsedMs);
        persistAnalysis(request, requiredRole, finalRecommendations, selectedStage, elapsedMs);
        return finalRecommendations;
    }

    /**
     * 每次分析完成后记录业务命中摘要，便于定位阶段命中情况。
     */
    private void logHitDiagnostics(String traceId,
                                   String requiredRole,
                                   String selectedStage,
                                   Map<String, Long> allyByRole,
                                   Map<String, Long> enemyByRole,
                                   Set<Long> unavailable,
                                   List<LastPickRecommendationDTO> recommendations,
                                   List<String> fallbackReasons,
                                   List<String> stageDiagnostics,
                                   long elapsedMs) {
        LastPickRecommendationDTO top = recommendations.isEmpty() ? null : recommendations.get(0);
        String topInfo = top == null
                ? "none"
                : top.getChampionName() + "#" + top.getChampionId()
                + "(wr=" + top.getWinRate() + ",games=" + top.getGames() + ")";

        log.info("LastPick hit traceId={}, role={}, stage={}, elapsedMs={}, top1={}, recCount={}, fallback={}, ally={}, enemy={}, unavailableCount={}, stages={}",
                traceId,
                requiredRole,
                selectedStage,
                elapsedMs,
                topInfo,
                recommendations.size(),
                String.join(" | ", fallbackReasons),
                compactRoleMap(allyByRole),
                compactRoleMap(enemyByRole),
                unavailable.size(),
                String.join(" || ", stageDiagnostics));
    }

    private Map<String, Long> compactRoleMap(Map<String, Long> byRole) {
        return byRole.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * 将 ES 聚合统计结果转换为推荐 DTO。
     */
    private List<LastPickRecommendationDTO> toRecommendations(List<CandidateStat> stats,
                                                              Map<Long, String> championNames,
                                                              StageSpec stage,
                                                              int topN,
                                                              String evidence,
                                                              String fallbackReason) {
        return stats.stream()
                // Same-stage ranking follows product rule: sort by win rate only.
                .sorted(Comparator.comparingDouble(CandidateStat::winRate).reversed())
                .limit(topN)
                .map(it -> LastPickRecommendationDTO.builder()
                        .championId(it.championId())
                        .championName(championNames.getOrDefault(it.championId(), "#" + it.championId()))
                        .winRate(round2(it.winRate()))
                        .games(it.games())
                        .levelUsed(stage.level())
                        .evidence(evidence)
                        .fallbackReason(fallbackReason == null || fallbackReason.isBlank() ? "" : fallbackReason)
                        .confidence(round2(confidence(stage, it.games()) * 100.0) / 100.0)
                        .build())
                .toList();
    }

    private double confidence(StageSpec stage, int games) {
        if (stage.minSamples() == null) {
            return Math.min(0.95, stage.baseConfidence() + Math.min(games, 20_000) / 20_000.0 * 0.10);
        }
        return Math.min(0.99, stage.baseConfidence() + Math.min(games, stage.minSamples()) / (double) stage.minSamples() * 0.05);
    }

    /**
     * 按阶段配置生成固定过滤条件。
     */
    private Map<String, String> fixedFiltersForStage(String stage,
                                                     String requiredRole,
                                                     Map<String, Long> ally,
                                                     Map<String, Long> enemy) {
        Map<String, String> filters = new LinkedHashMap<>();

        switch (stage) {
            case "P0" -> {
                putAllyAllExceptRequired(filters, ally, requiredRole);
                putEnemyByRoles(filters, enemy, List.of("TOP", "JUNGLE", "MID", "ADC", "SUP"));
            }
            case "P1" -> {
                putAllyByRoles(filters, ally, ALLY_CORE_BY_ROLE.getOrDefault(requiredRole, List.of()));
                putEnemyByRoles(filters, enemy, List.of("TOP", "JUNGLE", "MID", "ADC", "SUP"));
            }
            case "P2" -> {
                putAllyByRoles(filters, ally, ALLY_CORE_BY_ROLE.getOrDefault(requiredRole, List.of()));
                putEnemyByRoles(filters, enemy, ENEMY_CORE_BY_ROLE.getOrDefault(requiredRole, List.of()));
            }
            case "P3" -> {
                putAllyByRoles(filters, ally, ALLY_CORE_BY_ROLE.getOrDefault(requiredRole, List.of()));
                putEnemyByRoles(filters, enemy, List.of(requiredRole));
            }
            case "P4" -> putEnemyByRoles(filters, enemy, List.of(requiredRole));
            case "P5" -> {
                // baseline stage, no fixed filters
            }
            default -> throw new IllegalStateException("Unknown stage: " + stage);
        }
        return filters;
    }

    /**
     * 在 ES 中执行候选聚合查询。
     */
    private QueryResult queryCandidates(String requiredRole, Map<String, String> fixedFilters, Set<Long> unavailable) {
        try {
            String aggField = ALLY_FIELD_BY_ROLE.get(requiredRole);
            String body = buildAggQueryBody(aggField, fixedFilters);

            String response = client().post()
                    .uri("/{index}/_search", indexName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class).map(msg -> new IllegalStateException("ES query failed: " + msg)))
                    .bodyToMono(String.class)
                    .blockOptional()
                    .orElse("{}");

            JsonNode root = objectMapper.readTree(response);
            int totalGames = root.path("hits").path("total").path("value").asInt(0);

            List<CandidateStat> stats = new ArrayList<>();
            JsonNode buckets = root.path("aggregations").path("by_candidate").path("buckets");
            if (buckets.isArray()) {
                for (JsonNode bucket : buckets) {
                    long championId = parseChampionId(bucket.path("key").asText());
                    int games = bucket.path("doc_count").asInt(0);
                    int wins = bucket.path("wins").path("doc_count").asInt(0);
                    if (championId <= 0 || games <= 0 || unavailable.contains(championId)) {
                        continue;
                    }
                    double winRate = (wins * 100.0) / games;
                    stats.add(new CandidateStat(championId, winRate, games));
                }
            }

            return new QueryResult(totalGames, stats);
        } catch (Exception e) {
            log.error("ES candidate query failed. role={}, filters={}", requiredRole, fixedFilters, e);
            return new QueryResult(0, List.of());
        }
    }

    /**
     * 构建 terms + wins 子聚合查询体。
     */
    private String buildAggQueryBody(String aggField, Map<String, String> filters) {
        try {
            List<Map<String, Object>> filterList = new ArrayList<>();
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                filterList.add(Map.of("term", Map.of(entry.getKey(), entry.getValue())));
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("size", 0);
            body.put("track_total_hits", true);
            body.put("query", Map.of("bool", Map.of("filter", filterList)));
            body.put("aggs", Map.of(
                    "by_candidate", Map.of(
                            "terms", Map.of(
                                    "field", aggField,
                                    "size", ES_AGG_SIZE,
                                    "min_doc_count", ES_MIN_DOC_COUNT,
                                    "execution_hint", "map",
                                    "shard_size", ES_SHARD_SIZE
                            ),
                            "aggs", Map.of(
                                    "wins", Map.of("filter", Map.of("term", Map.of("win", true)))
                            )
                    )
            ));
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build ES query body", e);
        }
    }

    /**
     * 全阶段失败时的兜底策略：从英雄表按分路和胜率返回候选。
     */
    private List<LastPickRecommendationDTO> fallbackFromChampionTable(List<Champion> champions,
                                                                       String requiredRole,
                                                                       Set<Long> unavailable,
                                                                       int topN,
                                                                       String fallbackReason) {
        return champions.stream()
                .filter(c -> c.getId() != null)
                .filter(c -> !unavailable.contains(c.getId()))
                .filter(c -> roleMatchesChampion(c, requiredRole))
                .sorted(Comparator.comparingDouble((Champion c) -> c.getWinRate() == null ? 0.0 : c.getWinRate()).reversed())
                .limit(topN)
                .map(c -> LastPickRecommendationDTO.builder()
                        .championId(c.getId())
                        .championName(c.getName())
                        .winRate(round2(c.getWinRate() == null ? 50.0 : c.getWinRate()))
                        .games(0)
                        .levelUsed("P5")
                        .evidence("fallback from champion table")
                        .fallbackReason((fallbackReason == null ? "" : fallbackReason) + "; ES empty at all levels")
                        .confidence(0.35)
                        .build())
                .toList();
    }

    private boolean roleMatchesChampion(Champion c, String role) {
        String primary = normalizeRole(c.getPrimaryRole());
        String secondary = normalizeRole(c.getSecondaryRole());
        return role.equals(primary) || role.equals(secondary);
    }

    /**
     * 持久化分析结果（草稿 + 分析摘要）。
     */
    private void persistAnalysis(LastPickAnalysisRequestDTO request,
                                 String requiredRole,
                                 List<LastPickRecommendationDTO> recs,
                                 String stage,
                                 long elapsedMs) {
        if (recs.isEmpty()) {
            return;
        }

        Long bestId = recs.get(0).getChampionId();
        Map<String, Long> ally = allyByRole(request);
        ally.put(requiredRole, bestId);
        Map<String, Long> enemy = enemyByRole(request);

        Draft draft = new Draft();
        draft.setAllyTeamIds(joinTeamIds(ally));
        draft.setEnemyTeamIds(joinTeamIds(enemy));
        draft.setCreateTime(System.currentTimeMillis());
        draft.setRegion("GLOBAL");
        draftMapper.insert(draft);

        DraftAnalysis analysis = new DraftAnalysis();
        analysis.setDraftId(draft.getId());
        analysis.setMatchupScore(recs.get(0).getWinRate());
        analysis.setMatchupDetail(writeJsonSafe(recs));
        analysis.setFinalScore(recs.get(0).getWinRate());
        analysis.setRecommendation(recs.get(0).getChampionName() + " (" + stage + ")");
        analysis.setWinProbability(String.format(Locale.ROOT, "%.2f%%", recs.get(0).getWinRate()));
        analysis.setCreateTime(System.currentTimeMillis());
        analysis.setAnalysisTime(elapsedMs);
        analysisMapper.insert(analysis);
    }

    private String joinTeamIds(Map<String, Long> teamByRole) {
        return Arrays.asList("TOP", "JUNGLE", "MID", "ADC", "SUP").stream()
                .map(teamByRole::get)
                .filter(id -> id != null && id > 0)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String writeJsonSafe(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String evidence(String requiredRole, Map<String, String> filters) {
        if (filters.isEmpty()) {
            return "baseline by role=" + requiredRole;
        }
        return "role=" + requiredRole + ", filters=" + filters;
    }

    private Map<String, Long> allyByRole(LastPickAnalysisRequestDTO request) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (request == null) {
            return map;
        }
        map.put("TOP", request.getAllyTop());
        map.put("JUNGLE", request.getAllyJungle());
        map.put("MID", request.getAllyMid());
        map.put("ADC", request.getAllyAdc());
        map.put("SUP", request.getAllySup());
        return map;
    }

    private Map<String, Long> enemyByRole(LastPickAnalysisRequestDTO request) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (request == null) {
            return map;
        }
        map.put("TOP", request.getEnemyTop());
        map.put("JUNGLE", request.getEnemyJungle());
        map.put("MID", request.getEnemyMid());
        map.put("ADC", request.getEnemyAdc());
        map.put("SUP", request.getEnemySup());
        return map;
    }

    private Set<Long> unavailableChampions(LastPickAnalysisRequestDTO request,
                                           Map<String, Long> allyByRole,
                                           Map<String, Long> enemyByRole) {
        Set<Long> ids = new LinkedHashSet<>();
        if (request != null && request.getBannedChampions() != null) {
            ids.addAll(request.getBannedChampions().stream().filter(id -> id != null && id > 0).toList());
        }
        ids.addAll(allyByRole.values().stream().filter(id -> id != null && id > 0).toList());
        ids.addAll(enemyByRole.values().stream().filter(id -> id != null && id > 0).toList());
        return ids;
    }

    private void putAllyAllExceptRequired(Map<String, String> filters, Map<String, Long> allyByRole, String requiredRole) {
        for (Map.Entry<String, Long> entry : allyByRole.entrySet()) {
            if (entry.getKey().equals(requiredRole)) {
                continue;
            }
            if (entry.getValue() != null && entry.getValue() > 0) {
                filters.put(ALLY_FIELD_BY_ROLE.get(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
    }

    private void putAllyByRoles(Map<String, String> filters, Map<String, Long> allyByRole, List<String> roles) {
        for (String role : roles) {
            Long id = allyByRole.get(role);
            if (id != null && id > 0) {
                filters.put(ALLY_FIELD_BY_ROLE.get(role), String.valueOf(id));
            }
        }
    }

    private void putEnemyByRoles(Map<String, String> filters, Map<String, Long> enemyByRole, List<String> roles) {
        for (String role : roles) {
            Long id = enemyByRole.get(role);
            if (id != null && id > 0) {
                filters.put(ENEMY_FIELD_BY_ROLE.get(role), String.valueOf(id));
            }
        }
    }

    private int normalizeTopN(Integer topN) {
        if (topN == null) {
            return 5;
        }
        return Math.max(1, Math.min(20, topN));
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TOP" -> "TOP";
            case "JG", "JUNGLE" -> "JUNGLE";
            case "MID", "MIDDLE" -> "MID";
            case "ADC", "BOTTOM", "BOT" -> "ADC";
            case "SUP", "SUPPORT", "UTILITY" -> "SUP";
            default -> null;
        };
    }

    private long parseChampionId(String key) {
        try {
            return Long.parseLong(key);
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private WebClient client() {
        WebClient local = webClient;
        if (local == null) {
            synchronized (this) {
                local = webClient;
                if (local == null) {
                    String firstUri = esUris.split(",")[0].trim();
                    local = WebClient.builder().baseUrl(firstUri).build();
                    webClient = local;
                }
            }
        }
        return local;
    }

    private record StageSpec(String level, Integer minSamples, double baseConfidence) {
    }

    private record CandidateStat(long championId, double winRate, int games) {
    }

    private record QueryResult(int totalGames, List<CandidateStat> items) {
    }
}

