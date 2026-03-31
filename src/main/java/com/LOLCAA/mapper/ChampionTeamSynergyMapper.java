package com.LOLCAA.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChampionTeamSynergyMapper {

    Double findTeamSynergyScore(@Param("championIds") String championIds);

    String findDescription(@Param("championIds") String championIds);
}