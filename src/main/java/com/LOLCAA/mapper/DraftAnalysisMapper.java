package com.LOLCAA.mapper;

import com.LOLCAA.domain.po.DraftAnalysis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 草稿分析结果 Mapper。
 */
@Mapper
public interface DraftAnalysisMapper {

    /** 按 draft_id 查询分析结果。 */
    DraftAnalysis findByDraftId(@Param("draftId") Long draftId);

    int insert(DraftAnalysis analysis);

    int update(DraftAnalysis analysis);
}