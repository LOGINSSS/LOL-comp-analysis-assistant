package com.LOLCAA.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 红色方最后一选分析请求 DTO
 *
 * 0.4 版本将原本挂在 Controller 内部的请求对象上移到 domain.dto，
 * 作为 controller / service 共享的请求契约，后续可继续扩展 ban/pick 顺序、版本号、区服等字段。
 */
@Data
public class LastPickAnalysisRequestDTO {
    /** 已 ban 的英雄 ID 列表 */
    private List<Long> bannedChampions;

    /** 目标位置，例如 MID / JUNGLE / TOP / ADC / SUP */
    private String requiredRole;

    /** 返回候选数量（默认 5，最大 20） */
    private Integer topN;

    /** 己方阵容（位置已知，requiredRole 对应位置通常为空） */
    private Long allyTop;
    private Long allyJungle;
    private Long allyMid;
    private Long allyAdc;
    private Long allySup;

    /** 敌方阵容（位置已知） */
    private Long enemyTop;
    private Long enemyJungle;
    private Long enemyMid;
    private Long enemyAdc;
    private Long enemySup;
}
