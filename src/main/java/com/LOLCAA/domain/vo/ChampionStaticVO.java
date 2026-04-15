package com.LOLCAA.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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
    private Map<String, Integer> attributes;
}

