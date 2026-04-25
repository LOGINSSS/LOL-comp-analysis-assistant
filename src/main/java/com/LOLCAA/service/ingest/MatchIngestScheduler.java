package com.LOLCAA.service.ingest;

import com.LOLCAA.config.RiotIngestConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 采集调度入口。
 *
 * 负责在应用启动后进行可选索引初始化，并按 fixed-delay 定时执行采集。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchIngestScheduler {

    private final RiotIngestConfig ingestConfig;
    private final EsMatchIngestService esMatchIngestService;
    private final MatchIngestOrchestrator orchestrator;

    /**
     * 启动初始化：按配置决定是否检查/创建 ES 索引。
     */
    @PostConstruct
    public void init() {
        if (!ingestConfig.isEnabled()) {
            log.info("Riot ingest scheduler is disabled (riot.ingest.enabled=false)");
            return;
        }
        if (ingestConfig.isEnsureIndexOnStartup()) {
            esMatchIngestService.ensureIndex();
        }
    }

    /**
     * 定时执行一轮采集。
     */
    @Scheduled(fixedDelayString = "${riot.ingest.fixed-delay-ms:3600000}")
    public void scheduledIngest() {
        if (!ingestConfig.isEnabled()) {
            return;
        }

        MatchIngestOrchestrator.IngestRunResult result = orchestrator.runOnce();
        log.info("Riot ingest finished: fetchedMatches={}, indexedDocs={}, skippedMatches={}, message={}",
                result.fetchedMatches(), result.indexedDocs(), result.skippedMatches(), result.message());
    }
}
