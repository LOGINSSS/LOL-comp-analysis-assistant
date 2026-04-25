package com.LOLCAA.domain.dto.ingest;

import lombok.Data;

import java.util.List;

/**
 * Riot Match-V5 响应的精简 DTO。
 *
 * 仅保留采集/转换链路需要的字段，降低反序列化与维护成本。
 */
@Data
public class RiotMatchDto {
    private Metadata metadata;
    private Info info;

    @Data
    public static class Metadata {
        private String matchId;
    }

    @Data
    public static class Info {
        private String gameVersion;
        private Integer queueId;
        private Long gameEndTimestamp;
        private Long gameStartTimestamp;
        private List<Participant> participants;
        private List<Team> teams;
    }

    @Data
    public static class Participant {
        private Integer teamId;
        private Integer championId;
        private String teamPosition;
        private String individualPosition;
        private String lane;
    }

    @Data
    public static class Team {
        private Integer teamId;
        private Boolean win;
    }
}
