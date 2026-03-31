package com.LOLCAA.mapper;

import com.LOLCAA.domain.po.Champion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChampionMapper {

    Champion findById(@Param("id") Long id);

    List<Champion> findByIds(@Param("ids") List<Long> ids);

    List<Champion> findAll();

    int insert(Champion champion);

    int update(Champion champion);

    int deleteById(@Param("id") Long id);
}