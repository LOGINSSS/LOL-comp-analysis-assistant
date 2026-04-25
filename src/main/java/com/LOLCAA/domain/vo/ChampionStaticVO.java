package com.LOLCAA.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 英雄静态信息返回 VO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChampionStaticVO {
    private Long id;
    private String name;
    private String primaryRole;
    private String secondaryRole;
    private String tier;
    private Double pickRate;
    private Double winRate;
    private String imageUrl;

    /** 扩展属性（如能力标签）。 */
    private Map<String, Integer> attributes;
}
