package com.LOLCAA.mapper;

import com.LOLCAA.domain.po.Champion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 英雄基础信息 Mapper。
 */
@Mapper
public interface ChampionMapper {

    /** 按主键查询英雄。 */
    Champion findById(@Param("id") Long id);

    /** 按主键列表批量查询英雄。 */
    List<Champion> findByIds(@Param("ids") List<Long> ids);

    /** 查询全部英雄。 */
    List<Champion> findAll();

    /**
     * 按分路别名列表与名称关键词查询。
     * role 过滤在 XML 中支持 primary_role / secondary_role 两列。
     */
    List<Champion> findByRoleAndName(@Param("roles") List<String> roles, @Param("name") String name);

    int insert(Champion champion);

    int update(Champion champion);

    int deleteById(@Param("id") Long id);
}