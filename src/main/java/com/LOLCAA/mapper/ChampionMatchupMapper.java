package com.LOLCAA.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 英雄对位（matchup）数据查询 Mapper。
 */
@Mapper
public interface ChampionMatchupMapper {

    /**
     * 查询己方英雄集合对敌方英雄集合的历史胜率样本。
     */
    List<Double> findWinRates(
            @Param("ally") List<Long> ally,
            @Param("enemy") List<Long> enemy
    );

    /**
     * 在指定分路下查询对位胜率样本。
     */
    List<Double> findWinRatesByRole(
            @Param("ally") List<Long> ally,
            @Param("enemy") List<Long> enemy,
            @Param("role") String role
    );
}