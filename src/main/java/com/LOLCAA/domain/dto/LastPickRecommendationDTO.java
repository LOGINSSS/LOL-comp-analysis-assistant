package com.LOLCAA.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 最后一选推荐结果项 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastPickRecommendationDTO {
    /** 推荐英雄 ID。 */
    private Long championId;

    /** 推荐英雄名称。 */
    private String championName;

    /** 样本胜率（百分比，如 53.42）。 */
    private double winRate;

    /** 样本对局数。 */
    private int games;

    /** 命中的策略层级（P0~P5）。 */
    private String levelUsed;

    /** 证据描述（命中过滤条件摘要）。 */
    private String evidence;

    /** 回退原因（逐层退火轨迹）。 */
    private String fallbackReason;

    /** 置信度（0~1）。 */
    private double confidence;
}
