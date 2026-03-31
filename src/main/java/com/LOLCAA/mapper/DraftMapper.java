package com.LOLCAA.mapper;


import com.LOLCAA.domain.po.Draft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DraftMapper {

    Draft findById(@Param("id") Long id);

    int insert(Draft draft);
}