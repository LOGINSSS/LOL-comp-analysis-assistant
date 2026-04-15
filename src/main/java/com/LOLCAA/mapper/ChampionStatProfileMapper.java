package com.LOLCAA.mapper;

import com.LOLCAA.domain.po.ChampionStatProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChampionStatProfileMapper {

    ChampionStatProfile findByChampionId(@Param("championId") Long championId);

    List<ChampionStatProfile> findByChampionIds(@Param("championIds") List<Long> championIds);

    int insert(ChampionStatProfile profile);

    int update(ChampionStatProfile profile);

    int deleteByChampionId(@Param("championId") Long championId);
}
