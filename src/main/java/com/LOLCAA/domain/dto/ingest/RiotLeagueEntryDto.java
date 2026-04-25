package com.LOLCAA.domain.dto.ingest;

import lombok.Data;

/**
 * 榜单条目 DTO，仅保留召唤师链路必要字段。
 */
@Data
public class RiotLeagueEntryDto {
    /** Challenger 条目直接返回的全局唯一玩家标识（主链路使用）。 */
    private String puuid;

    /** 旧接口字段，部分环境可能为空，保留兼容。 */
    private String summonerId;

    /** 展示名在部分环境可能缺失。 */
    private String summonerName;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;
    private String rank;
}

