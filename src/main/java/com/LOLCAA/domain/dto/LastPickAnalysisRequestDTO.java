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

    /** 己方已选英雄 ID 列表（最后一选前通常为 4 个） */
    private List<Long> allyPickedChampions;

    /** 敌方已选英雄 ID 列表（最后一选前通常为 5 个） */
    private List<Long> enemyPickedChampions;

    /** 需求位置，例如 MID / JUNGLE / TOP / ADC / SUP */
    private String requiredRole;
}
