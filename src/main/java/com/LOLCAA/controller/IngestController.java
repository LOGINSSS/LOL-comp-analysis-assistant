package com.LOLCAA.controller;

import com.LOLCAA.config.RiotIngestConfig;
import com.LOLCAA.service.ingest.EsMatchIngestService;
import com.LOLCAA.service.ingest.MatchIngestOrchestrator;
import com.LOLCAA.service.ingest.SummonerPoolRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 采集运维接口。
 *
 * 用于手动触发索引初始化与单轮采集，方便联调和故障排查。
 */
@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
public class IngestController {

    private final RiotIngestConfig ingestConfig;
    private final EsMatchIngestService esMatchIngestService;
    private final SummonerPoolRefreshService summonerPoolRefreshService;
    private final MatchIngestOrchestrator orchestrator;

    /**
     * 手动检查并创建 ES 索引（若不存在）。
     */
    @PostMapping("/ensure-index")
    public Map<String, Object> ensureIndex() {
        esMatchIngestService.ensureIndex();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("index", ingestConfig.getIndexName());
        return result;
    }

    /**
     * 手动执行一轮采集流程。
     */
    @PostMapping("/run-once")
    public MatchIngestOrchestrator.IngestRunResult runOnce() {
        return orchestrator.runOnce();
    }

    /**
     * 手动刷新召唤师池（Challenger -> puuid）。
     */
    @PostMapping("/refresh-pool")
    public SummonerPoolRefreshService.RefreshResult refreshPool() {
        // 手动触发默认强制刷新，避免被最小间隔拦截。
        return summonerPoolRefreshService.refreshTopPool(true);
    }
}
