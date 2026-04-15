package com.LOLCAA.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 英雄池列表响应 DTO（面向前端）
 */
@Data
@AllArgsConstructor
public class ChampionListItemDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private String primaryRole;
    private String secondaryRole;
}

