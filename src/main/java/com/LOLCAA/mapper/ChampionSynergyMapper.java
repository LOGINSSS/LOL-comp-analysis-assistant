package com.LOLCAA.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChampionSynergyMapper {

    List<Double> findSynergyWinRates(
            @Param("championIds") List<Long> championIds
    );

    Double findPairWinRate(
            @Param("c1") Long c1,
            @Param("c2") Long c2
    );
}