package com.LOLCAA.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 已处理对局记录 Mapper（用于幂等控制）。
 */
@Mapper
public interface ProcessedMatchMapper {

    /** 查询某 match_id 是否已处理。 */
    @Select("SELECT COUNT(*) FROM processed_match WHERE match_id = #{matchId}")
    Integer countByMatchId(@Param("matchId") String matchId);

    /**
     * 幂等写入已处理记录；若已存在则忽略。
     */
    @Insert("INSERT INTO processed_match(match_id, processed_at) VALUES(#{matchId}, NOW()) ON CONFLICT (match_id) DO NOTHING")
    int insertIgnore(@Param("matchId") String matchId);
}
