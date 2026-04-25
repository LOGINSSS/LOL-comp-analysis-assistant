package com.LOLCAA.service.ingest;

import com.LOLCAA.domain.dto.ingest.MatchDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * ES 对局索引服务。
 *
 * 负责索引存在性检查、索引创建以及批量写入 `lol_matches` 文档。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EsMatchIngestService {
    /**
     * 预定义mapping
     */
    private static final String LOL_MATCHES_MAPPING = """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0,
                "refresh_interval": "5s"
              },
              "mappings": {
                "dynamic": "strict",
                "properties": {
                  "matchId": {"type": "keyword"},
                  "side": {"type": "keyword"},
                  "win": {"type": "boolean"},
                  "top": {"type": "keyword", "eager_global_ordinals": true},
                  "jungle": {"type": "keyword", "eager_global_ordinals": true},
                  "mid": {"type": "keyword", "eager_global_ordinals": true},
                  "adc": {"type": "keyword", "eager_global_ordinals": true},
                  "sup": {"type": "keyword", "eager_global_ordinals": true},
                  "enemyTop": {"type": "keyword", "eager_global_ordinals": true},
                  "enemyJungle": {"type": "keyword", "eager_global_ordinals": true},
                  "enemyMid": {"type": "keyword", "eager_global_ordinals": true},
                  "enemyAdc": {"type": "keyword", "eager_global_ordinals": true},
                  "enemySup": {"type": "keyword", "eager_global_ordinals": true}
                }
              }
            }
            """;

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUris;

    @Value("${riot.ingest.index-name:lol_matches}")
    private String indexName;

    private final ObjectMapper objectMapper;
    private volatile WebClient webClient;

    /**
     * 确保目标索引存在；不存在时按预定义 mapping 创建。
     */
    public void ensureIndex() {
        int status = client().head().uri("/{index}", indexName).exchangeToMono(resp -> resp.releaseBody().thenReturn(resp.statusCode().value())).blockOptional().orElse(500);
        if (status == 200) {
            return;
        }
        if (status != 404) {
            throw new IllegalStateException("Unexpected ES status when checking index: " + status);
        }

        String body = client().put()
                .uri("/{index}", indexName)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(LOL_MATCHES_MAPPING)
                .retrieve()
                .bodyToMono(String.class)
                .blockOptional()
                .orElse("");
        log.info("Created index {} response={}", indexName, body);
    }

    /**
     * 批量 upsert 对局视角文档。
     *
     * @return 本次尝试写入的文档数
     */
    public int bulkUpsert(List<MatchDocument> docs) {
        if (docs == null || docs.isEmpty()) {
            return 0;
        }

        String payload = toBulkPayload(docs);
        String response = client().post()
                .uri(uriBuilder -> uriBuilder.path("/_bulk").queryParam("refresh", "false").build())
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class).map(body -> new IllegalStateException("ES bulk failed: " + body)))
                .bodyToMono(String.class)
                .blockOptional()
                .orElse("{}");

        if (response.contains("\"errors\":true")) {
            log.warn("ES bulk response has item failures: {}", response);
        }
        return docs.size();
    }

    /**
     * 组装 ES Bulk API 所需的 ndjson 载荷。
     */
    private String toBulkPayload(List<MatchDocument> docs) {
        StringBuilder sb = new StringBuilder();
        for (MatchDocument doc : docs) {
            String matchId = doc.getMatchId();
            String side = doc.getSide();
            if (matchId == null || side == null) {
                continue;
            }
            String id = matchId + "_" + side;
            sb.append("{\"index\":{\"_index\":\"")
                    .append(indexName)
                    .append("\",\"_id\":\"")
                    .append(id)
                    .append("\"}}\n");
            try {
                sb.append(objectMapper.writeValueAsString(doc)).append('\n');
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize MatchDocument", e);
            }
        }
        return sb.toString();
    }

    /**
     * 懒加载 ES WebClient，默认使用 spring.elasticsearch.uris 的第一个地址。
     */
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
}
