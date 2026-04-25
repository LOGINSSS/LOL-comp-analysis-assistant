package com.LOLCAA.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 多英雄阵容协同 Mapper。
 */
@Mapper
public interface ChampionTeamSynergyMapper {

    /** 根据排序后的 championIds 字符串查询阵容协同分。 */
    Double findTeamSynergyScore(@Param("championIds") String championIds);

    /** 查询阵容协同描述文案。 */
    String findDescription(@Param("championIds") String championIds);
}