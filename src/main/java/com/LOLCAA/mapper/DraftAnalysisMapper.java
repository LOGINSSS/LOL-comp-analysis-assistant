package com.LOLCAA.mapper;


import com.LOLCAA.domain.po.DraftAnalysis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DraftAnalysisMapper {

    DraftAnalysis findByDraftId(@Param("draftId") Long draftId);

    int insert(DraftAnalysis analysis);

    int update(DraftAnalysis analysis);
}