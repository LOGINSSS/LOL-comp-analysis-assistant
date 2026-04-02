package com.LOLCAA.service;

import com.LOLCAA.domain.dto.LastPickAnalysisRequestDTO;
import com.LOLCAA.domain.po.DraftAnalysis;

/**
 * 红色方最后一选分析服务（Demo版本）
 * 专注于LOL游戏最后一选的英雄推荐分析
 */
public interface LastPickAnalysisService {

    /**
     * 分析红色方最后一选英雄
     * @param request 最后一选分析请求
     * @return 分析结果
     */
    DraftAnalysis analyzeLastPick(LastPickAnalysisRequestDTO request);
}
