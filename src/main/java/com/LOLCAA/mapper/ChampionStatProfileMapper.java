package com.LOLCAA.mapper;

import com.LOLCAA.domain.po.ChampionStatProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChampionStatProfileMapper {

    ChampionStatProfile findByChampionId(@Param("championId") Long championId);

    int insert(ChampionStatProfile profile);

    int update(ChampionStatProfile profile);

    int deleteByChampionId(@Param("championId") Long championId);
}
