package com.LOLCAA.mapper;

import com.LOLCAA.domain.po.Draft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * BP 草稿记录 Mapper。
 */
@Mapper
public interface DraftMapper {

    /** 按主键查询一条草稿记录。 */
    Draft findById(@Param("id") Long id);

    /** 插入草稿记录。 */
    int insert(Draft draft);
}