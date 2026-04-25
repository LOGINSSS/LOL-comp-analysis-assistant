package com.LOLCAA.service.ingest;

import com.LOLCAA.config.RiotIngestConfig;
import com.LOLCAA.domain.dto.ingest.MatchDocument;
import com.LOLCAA.domain.dto.ingest.RiotMatchDto;
import com.LOLCAA.mapper.IngestProgressMapper;
import com.LOLCAA.mapper.ProcessedMatchMapper;
import com.LOLCAA.mapper.SummonerPoolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 采集编排器：负责串联“拉取 -> 转换 -> 入库/入索引”全过程。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchIngestOrchestrator {

    private final RiotIngestConfig ingestConfig;
    private final RiotApiClient riotApiClient;
    private final MatchDocumentConverter converter;
    private final EsMatchIngestService esMatchIngestService;
    private final SummonerPoolRefreshService summonerPoolRefreshService;
    private final SummonerPoolMapper summonerPoolMapper;
    private final ProcessedMatchMapper processedMatchMapper;
    private final IngestProgressMapper ingestProgressMapper;

    /**
     * 执行一轮增量采集。
     */
    public IngestRunResult runOnce() {
        StringBuilder messageBuilder = new StringBuilder("ok");
        // 每轮先刷新一次韩服榜单池，保证后续拉取使用最新 puuid。
        if (ingestConfig.isRefreshPoolBeforeIngest()) {
            try {
                SummonerPoolRefreshService.RefreshResult refreshResult = summonerPoolRefreshService.refreshTopPool();
                messageBuilder.append("; poolRefresh upsert=").append(refreshResult.upsertedRows())
                        .append(" unresolved=").append(refreshResult.unresolvedEntries());
            } catch (Exception ex) {
                // 刷新失败时不中断主链路，继续使用库里现有 puuid，避免全量不可用。
                log.warn("Summoner pool refresh failed, fallback to existing pool", ex);
                messageBuilder.append("; poolRefresh failed");
            }
        }

        //1) 读取 puuid 池；
        List<String> puuids = summonerPoolMapper.findPuuids(ingestConfig.getPuuidBatchLimit());
        if (puuids == null || puuids.isEmpty()) {
            return new IngestRunResult(0, 0, 0, "summoner_pool is empty");
        }

        int fetchedMatches = 0;
        int indexedDocs = 0;
        int skippedMatches = 0;

        for (String puuid : puuids) {
            if (puuid == null || puuid.isBlank()) {
                continue;
            }

            Long lastTs = ingestProgressMapper.findLastMatchEndTs(puuid);
            long maxSeenTs = lastTs == null ? 0L : lastTs;
            // 仅拉取上次结束时间之后的对局，避免反复扫描旧记录。
            Long startTimeSeconds = lastTs == null ? null : (lastTs / 1000L) + 1L;
            // 2) 按进度增量拉取 matchId；
            List<String> matchIds = riotApiClient.getMatchIdsByPuuid(
                    puuid,
                    0,
                    ingestConfig.getPerPuuidCount(),
                    ingestConfig.getQueue(),
                    ingestConfig.getMatchType(),
                    startTimeSeconds
            );
            //3) 幂等过滤已处理比赛；
            for (String matchId : matchIds) {
                if (alreadyProcessed(matchId)) {
                    skippedMatches++;
                    continue;
                }

                RiotMatchDto match = riotApiClient.getMatchDetails(matchId);
                if (match == null || match.getInfo() == null) {
                    skippedMatches++;
                    continue;
                }

                long endTs = match.getInfo().getGameEndTimestamp() == null ? 0L : match.getInfo().getGameEndTimestamp();
                if (lastTs != null && endTs > 0 && endTs <= lastTs) {
                    skippedMatches++;
                    continue;
                }
                // 4) 拉取详情并写入 ES；
                // 写入 ES 成功后再标记 processed_match，避免“先标记后失败”造成数据丢失。
                List<MatchDocument> docs = converter.toPerspectiveDocuments(match);
                indexedDocs += esMatchIngestService.bulkUpsert(docs);
                // 5) 更新 processed_match 。
                processedMatchMapper.insertIgnore(matchId);
                fetchedMatches++;
                maxSeenTs = Math.max(maxSeenTs, endTs);
            }
            //6)更新 ingest_progress。
            if (maxSeenTs > 0) {
                ingestProgressMapper.upsert(puuid, maxSeenTs);
            }

            // 标记该 puuid 本轮已调度，下一轮优先处理更久未更新的召唤师。
            summonerPoolMapper.touchUpdatedAt(puuid);
        }

        return new IngestRunResult(fetchedMatches, indexedDocs, skippedMatches, messageBuilder.toString());
    }

    /**
     * match_id 幂等判断。
     */
    private boolean alreadyProcessed(String matchId) {
        Integer count = processedMatchMapper.countByMatchId(matchId);
        return count != null && count > 0;
    }

    /**
     * 单轮采集统计结果。
     */
    public record IngestRunResult(int fetchedMatches, int indexedDocs, int skippedMatches, String message) {
    }
}
