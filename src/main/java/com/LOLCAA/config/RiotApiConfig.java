package com.LOLCAA.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Riot API 访问配置。
 * 对应 application.yaml 中的 `riot.api.*`。
 */
@Component
@ConfigurationProperties(prefix = "riot.api")
@Data
public class RiotApiConfig {
    /** Riot 开发者 API Key。 */
    private String key;

    /** 区域路由（Match V5 常用 asia/europe/americas）。 */
    private String region = "asia";

    /** 平台路由（如 KR、JP1，仅部分接口会用到）。 */
    private String platform = "KR";

    /** 可重试请求的最大重试次数（429/5xx）。 */
    private int maxRetries = 3;

    /** 每秒请求上限（Riot 开发 Key 通常为 20）。 */
    private int rateLimitPerSecond = 20;

    /** 两分钟窗口请求上限（Riot 开发 Key 通常为 100）。 */
    private int rateLimitPerTwoMinutes = 100;
}
