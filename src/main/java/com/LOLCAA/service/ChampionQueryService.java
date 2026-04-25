package com.LOLCAA.service;

import com.LOLCAA.domain.dto.ChampionListItemDTO;

import java.util.List;

/**
 * 英雄列表查询服务。
 */
public interface ChampionQueryService {

    /**
     * 按分路/名称查询英雄。
     *
     * 说明：当前实现中 role 为空或无效时会回退到默认分路（TOP）。
     */
    List<ChampionListItemDTO> listChampions(String role, String name);
}
