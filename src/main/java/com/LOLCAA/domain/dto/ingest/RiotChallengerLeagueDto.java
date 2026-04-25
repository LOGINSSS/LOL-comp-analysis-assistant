package com.LOLCAA.domain.dto.ingest;

import lombok.Data;

import java.util.List;

/**
 * Challenger 榜单响应 DTO（League-V4）。
 */
@Data
public class RiotChallengerLeagueDto {
    private String tier;
    private String queue;
    private String name;
    private List<RiotLeagueEntryDto> entries;
}

