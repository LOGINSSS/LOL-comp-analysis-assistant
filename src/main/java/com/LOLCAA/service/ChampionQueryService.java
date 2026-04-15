package com.LOLCAA.service;

import com.LOLCAA.domain.dto.ChampionListItemDTO;

import java.util.List;

/**
 * 英雄列表查询服务
 */
public interface ChampionQueryService {

    /**
     * 按位置/名称查询英雄。
     * 当 role 为空时，默认查询 TOP。
     */
    List<ChampionListItemDTO> listChampions(String role, String name);
}

