package com.LOLCAA.service.ingest;

import com.LOLCAA.config.RiotIngestConfig;
import com.LOLCAA.domain.dto.ingest.RiotLeagueEntryDto;
import com.LOLCAA.mapper.SummonerPoolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 召唤师池刷新服务。
 *
 * 链路：
 * 1) 拉 Challenger 榜单；
 * 2) 直接提取 puuid（新版榜单字段）；
 * 3) 批量写入 summoner_pool。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SummonerPoolRefreshService {

    private final RiotIngestConfig ingestConfig;
    private final RiotApiClient riotApiClient;
    private final SummonerPoolMapper summonerPoolMapper;
    private volatile long lastRefreshAtMs = 0L;

    /**
     * 刷新韩服召唤师池（目标 TopN 默认 500）。
     */
    public RefreshResult refreshTopPool() {
        return refreshTopPool(false);
    }

    /**
     * 刷新韩服召唤师池。
     *
     * @param force true=忽略最小刷新间隔（供手动接口使用）
     */
    public synchronized RefreshResult refreshTopPool(boolean force) {
        long now = System.currentTimeMillis();
        long minInterval = Math.max(0L, ingestConfig.getPoolRefreshMinIntervalMs());
        if (!force && minInterval > 0 && now - lastRefreshAtMs < minInterval) {
            return new RefreshResult(0, 0, 0, 0, "skip: refreshed recently");
        }

        List<RiotLeagueEntryDto> entries = riotApiClient.getChallengerLeagueEntries(ingestConfig.getLeagueQueue());
        if (entries == null || entries.isEmpty()) {
            return new RefreshResult(0, 0, 0, 0, "challenger league is empty");
        }

        // 先按 LP 倒序，保证每次优先拿榜单前段。
        List<RiotLeagueEntryDto> topEntries = entries.stream()
                .sorted(Comparator.comparingInt((RiotLeagueEntryDto it) -> it.getLeaguePoints() == null ? 0 : it.getLeaguePoints()).reversed())
                .limit(Math.max(1, ingestConfig.getPoolTargetSize()))
                .toList();

        // 在内存按 puuid 去重，避免同一批次重复 upsert 同一个主键。
        Map<String, SummonerPoolMapper.SummonerPoolUpsertRow> dedupRows = new LinkedHashMap<>(topEntries.size());
        int unresolved = 0;
        for (RiotLeagueEntryDto entry : topEntries) {
            if (entry == null || entry.getPuuid() == null || entry.getPuuid().isBlank()) {
                unresolved++;
                continue;
            }

            // 部分环境下 summonerName 可能缺失；回退 puuid 便于排查与展示。
            String displayName = (entry.getSummonerName() == null || entry.getSummonerName().isBlank())
                    ? entry.getPuuid()
                    : entry.getSummonerName();
            dedupRows.putIfAbsent(
                    entry.getPuuid(),
                    new SummonerPoolMapper.SummonerPoolUpsertRow(entry.getPuuid(), displayName, "CHALLENGER")
            );
        }

        List<SummonerPoolMapper.SummonerPoolUpsertRow> rows = new ArrayList<>(dedupRows.values());

        int upserted = rows.isEmpty() ? 0 : summonerPoolMapper.batchUpsert(rows);
        String message = rows.isEmpty() ? "no valid puuid from challenger entries" : "ok";
        lastRefreshAtMs = now;
        log.info("Summoner pool refresh finished: sourceEntries={}, selectedEntries={}, resolvedPuuid={}, unresolved={}",
                entries.size(), topEntries.size(), rows.size(), unresolved);
        return new RefreshResult(entries.size(), topEntries.size(), upserted, unresolved, message);
    }

    /**
     * 单次刷新统计。
     */
    public record RefreshResult(int sourceEntries, int selectedEntries, int upsertedRows, int unresolvedEntries, String message) {
    }
}



