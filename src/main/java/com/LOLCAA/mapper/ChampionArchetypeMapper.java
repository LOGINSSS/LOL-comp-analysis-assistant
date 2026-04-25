package com.LOLCAA.mapper;

import com.LOLCAA.domain.po.ChampionArchetype;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 英雄原型标签（archetype）Mapper。
 */
@Mapper
public interface ChampionArchetypeMapper {

    ChampionArchetype findByChampionId(@Param("championId") Long championId);

    int insert(ChampionArchetype archetype);

    int update(ChampionArchetype archetype);

    int deleteByChampionId(@Param("championId") Long championId);
}
