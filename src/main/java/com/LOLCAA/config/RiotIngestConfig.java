package com.LOLCAA.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Riot 对局采集任务配置。
 * 对应 application.yaml 中的 `riot.ingest.*`。
 */
@Component
@ConfigurationProperties(prefix = "riot.ingest")
@Data
public class RiotIngestConfig {
    /** 是否启用定时采集。 */
    private boolean enabled = false;

    /** 定时任务间隔（毫秒）。 */
    private long fixedDelayMs = 3_600_000L;

    /** 启动时是否自动检查/创建 ES 索引。 */
    private boolean ensureIndexOnStartup = true;

    /** ES 目标索引名。 */
    private String indexName = "lol_matches";

    /** 每个 puuid 单轮最多拉取的比赛数量。 */
    private int perPuuidCount = 20;

    /** 队列过滤 420=单双排。 */
    private Integer queue = 420;

    /** 对局类型过滤（如 ranked）。 */
    private String matchType = "ranked";

    /** 每轮最多处理多少个 puuid。 */
    private int puuidBatchLimit = 50;

    /**
     * 是否在每轮采集前先刷新召唤师池。
     * 开启后会先调用 Challenger 榜单链路，更新池中的 puuid。
     */
    private boolean refreshPoolBeforeIngest = true;

    /** 榜单接口使用的队列类型。 */
    private String leagueQueue = "RANKED_SOLO_5x5";

    /** 每次从榜单中最多写入多少个韩服召唤师到池。 */
    private int poolTargetSize = 500;

    /** 召唤师池最小刷新间隔（毫秒），用于抑制高频重复刷新。 */
    private long poolRefreshMinIntervalMs = 300_000L;
}
