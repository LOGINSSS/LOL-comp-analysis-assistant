package com.LOLCAA.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 采集召唤师池 Mapper。
 */
@Mapper
public interface SummonerPoolMapper {

    /** 召唤师池写入行模型。 */
    record SummonerPoolUpsertRow(String puuid, String summonerName, String tier) {
    }

    /**
     * 读取最久未更新的一批 puuid，避免每轮命中同一批召唤师。
     */
    @Select("SELECT puuid FROM summoner_pool ORDER BY updated_at ASC NULLS FIRST, puuid ASC LIMIT #{limit}")
    List<String> findPuuids(@Param("limit") int limit);

    /**
     * 标记该召唤师本轮已处理，用于下轮轮转。
     */
    @Update("UPDATE summoner_pool SET updated_at = NOW() WHERE puuid = #{puuid}")
    int touchUpdatedAt(@Param("puuid") String puuid);

    /**
     * 批量写入/更新召唤师池。
     */
    @Insert("""
            <script>
            INSERT INTO summoner_pool(puuid, summoner_name, tier, updated_at)
            VALUES
            <foreach collection='rows' item='row' separator=','>
                (#{row.puuid}, #{row.summonerName}, #{row.tier}, NOW())
            </foreach>
            ON CONFLICT (puuid)
            DO UPDATE SET
                summoner_name = EXCLUDED.summoner_name,
                tier = EXCLUDED.tier,
                updated_at = NOW()
            WHERE summoner_pool.summoner_name IS DISTINCT FROM EXCLUDED.summoner_name
               OR summoner_pool.tier IS DISTINCT FROM EXCLUDED.tier
            </script>
            """)
    int batchUpsert(@Param("rows") List<SummonerPoolUpsertRow> rows);
}
