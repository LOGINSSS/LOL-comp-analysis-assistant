package com.LOLCAA.service.ingest;

import com.LOLCAA.config.RiotApiConfig;
import com.LOLCAA.domain.dto.ingest.RiotChallengerLeagueDto;
import com.LOLCAA.domain.dto.ingest.RiotLeagueEntryDto;
import com.LOLCAA.domain.dto.ingest.RiotMatchDto;
import com.LOLCAA.domain.dto.ingest.RiotSummonerDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Riot API 客户端。
 *
 * 负责：
 * 1) 拉取比赛 ID 列表；
 * 2) 拉取比赛详情；
 * 3) 统一处理限流与重试策略。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiotApiClient {

    private final RiotApiConfig config;
    private volatile WebClient matchWebClient;
    private volatile WebClient platformWebClient;
    private volatile DualWindowRateLimiter limiter;

    /**
     * 拉取 Challenger 榜单条目。
     *
     * 注意：Challenger 实际人数可能小于 500，因此这里只返回接口实际数据。
     */
    public List<RiotLeagueEntryDto> getChallengerLeagueEntries(String queue) {
        String leagueQueue = (queue == null || queue.isBlank()) ? "RANKED_SOLO_5x5" : queue;

        limiter().acquire();
        try {
            RiotChallengerLeagueDto response = platformClient().get()
                    .uri("/lol/league/v4/challengerleagues/by-queue/{queue}", leagueQueue)
                    .retrieve()
                    .bodyToMono(RiotChallengerLeagueDto.class)
                    .retryWhen(retrySpec())
                    .block();
            if (response == null || response.getEntries() == null) {
                return Collections.emptyList();
            }
            return response.getEntries();
        } catch (WebClientResponseException ex) {
            log.error("Failed to fetch challenger league. queue={}, status={}, body={}",
                    leagueQueue, ex.getStatusCode(), ex.getResponseBodyAsString());
            return Collections.emptyList();
        }
    }

    /**
     * 用 summonerId 反查 puuid（Summoner-V4）。
     */
    public RiotSummonerDto getSummonerById(String summonerId) {
        if (summonerId == null || summonerId.isBlank()) {
            return null;
        }

        limiter().acquire();
        try {
            return platformClient().get()
                    .uri("/lol/summoner/v4/summoners/{summonerId}", summonerId)
                    .retrieve()
                    .bodyToMono(RiotSummonerDto.class)
                    .retryWhen(retrySpec())
                    .block();
        } catch (WebClientResponseException ex) {
            log.warn("Skip summonerId={}, status={}, body={}", summonerId, ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        }
    }

    /**
     * 按 puuid 拉取比赛 ID 列表。
     */
    public List<String> getMatchIdsByPuuid(String puuid,
                                           int start,
                                           int count,
                                           Integer queue,
                                           String matchType,
                                           Long startTimeEpochSeconds) {
        if (puuid == null || puuid.isBlank()) {
            return Collections.emptyList();
        }

        limiter().acquire();
        try {
            return client().get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/lol/match/v5/matches/by-puuid/{puuid}/ids")
                                .queryParam("start", Math.max(0, start))
                                .queryParam("count", Math.max(1, Math.min(100, count)));
                        if (queue != null) {
                            builder.queryParam("queue", queue);
                        }
                        if (matchType != null && !matchType.isBlank()) {
                            builder.queryParam("type", matchType);
                        }
                        if (startTimeEpochSeconds != null && startTimeEpochSeconds > 0) {
                            builder.queryParam("startTime", startTimeEpochSeconds);
                        }
                        return builder.build(puuid);
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                    .retryWhen(retrySpec())
                    .blockOptional()
                    .orElse(Collections.emptyList());
        } catch (WebClientResponseException ex) {
            log.error("Failed to fetch match IDs for puuid={}, status={}, body={}", puuid, ex.getStatusCode(), ex.getResponseBodyAsString());
            return Collections.emptyList();
        }
    }

    /**
     * 拉取单场比赛详情。
     */
    public RiotMatchDto getMatchDetails(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return null;
        }

        limiter().acquire();
        try {
            return client().get()
                    .uri("/lol/match/v5/matches/{matchId}", matchId)
                    .retrieve()
                    .bodyToMono(RiotMatchDto.class)
                    .retryWhen(retrySpec())
                    .block();
        } catch (WebClientResponseException ex) {
            log.warn("Skip match={}, status={}, body={}", matchId, ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        }
    }

    /**
     * 懒加载 WebClient，避免在配置未准备好时提前初始化。
     */
    private WebClient client() {
        WebClient local = matchWebClient;
        if (local == null) {
            synchronized (this) {
                local = matchWebClient;
                if (local == null) {
                    local = WebClient.builder()
                            .baseUrl("https://" + config.getRegion() + ".api.riotgames.com")
                            .defaultHeader("X-Riot-Token", config.getKey())
                            .build();
                    matchWebClient = local;
                }
            }
        }
        return local;
    }

    /**
     * 平台路由客户端（用于 League-V4、Summoner-V4）。
     */
    private WebClient platformClient() {
        WebClient local = platformWebClient;
        if (local == null) {
            synchronized (this) {
                local = platformWebClient;
                if (local == null) {
                    String platform = config.getPlatform() == null ? "KR" : config.getPlatform();
                    local = WebClient.builder()
                            .baseUrl("https://" + platform.toLowerCase() + ".api.riotgames.com")
                            .defaultHeader("X-Riot-Token", config.getKey())
                            .build();
                    platformWebClient = local;
                }
            }
        }
        return local;
    }

    /**
     * 懒加载双窗口限流器，按配置动态生效。
     */
    private DualWindowRateLimiter limiter() {
        DualWindowRateLimiter local = limiter;
        if (local == null) {
            synchronized (this) {
                local = limiter;
                if (local == null) {
                    local = new DualWindowRateLimiter(config.getRateLimitPerSecond(), config.getRateLimitPerTwoMinutes());
                    limiter = local;
                }
            }
        }
        return local;
    }

    /**
     * 仅对限流与服务端错误进行指数退避重试。
     */
    private Retry retrySpec() {
        return Retry.backoff(config.getMaxRetries(), Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(8))
                .filter(this::isRetryable)
                .doBeforeRetry(signal -> log.warn("Riot API retry #{} cause={}", signal.totalRetriesInARow() + 1, signal.failure().toString()));
    }

    private boolean isRetryable(Throwable throwable) {
        if (!(throwable instanceof WebClientResponseException ex)) {
            return false;
        }
        HttpStatusCode code = ex.getStatusCode();
        int value = code.value();
        return value == 429 || value >= 500;
    }
}
