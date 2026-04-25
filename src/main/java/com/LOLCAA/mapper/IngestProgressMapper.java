package com.LOLCAA.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 采集进度 Mapper（断点续传）。
 */
@Mapper
public interface IngestProgressMapper {

    /** 读取某 puuid 最近已处理比赛的结束时间戳（毫秒）。 */
    @Select("SELECT last_match_end_ts FROM ingest_progress WHERE puuid = #{puuid}")
    Long findLastMatchEndTs(@Param("puuid") String puuid);

    /**
     * 写入或更新采集进度。
     */
    @Insert("""
            INSERT INTO ingest_progress(puuid, last_match_end_ts, updated_at)
            VALUES(#{puuid}, #{lastMatchEndTs}, NOW())
            ON CONFLICT (puuid)
            DO UPDATE SET last_match_end_ts = EXCLUDED.last_match_end_ts, updated_at = NOW()
            """)
    int upsert(@Param("puuid") String puuid, @Param("lastMatchEndTs") long lastMatchEndTs);
}
