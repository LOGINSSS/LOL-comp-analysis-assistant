package com.LOLCAA.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChampionMatchupMapper {

    List<Double> findWinRates(
            @Param("ally") List<Long> ally,
            @Param("enemy") List<Long> enemy
    );

    List<Double> findWinRatesByRole(
            @Param("ally") List<Long> ally,
            @Param("enemy") List<Long> enemy,
            @Param("role") String role
    );
}