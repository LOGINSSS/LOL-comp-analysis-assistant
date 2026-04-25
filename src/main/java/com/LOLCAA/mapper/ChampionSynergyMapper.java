package com.LOLCAA.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 双英雄协同（synergy）数据 Mapper。
 */
@Mapper
public interface ChampionSynergyMapper {

    /** 查询英雄集合内部的协同胜率样本。 */
    List<Double> findSynergyWinRates(
            @Param("championIds") List<Long> championIds
    );

    /** 查询指定双英雄组合的协同胜率。 */
    Double findPairWinRate(
            @Param("c1") Long c1,
            @Param("c2") Long c2
    );
}