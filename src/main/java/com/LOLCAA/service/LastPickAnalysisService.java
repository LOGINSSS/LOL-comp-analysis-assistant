package com.LOLCAA.service;

import com.LOLCAA.domain.dto.LastPickAnalysisRequestDTO;
import com.LOLCAA.domain.dto.LastPickRecommendationDTO;

import java.util.List;

/**
 * 最后一选推荐分析服务。
 */
public interface LastPickAnalysisService {

    /**
     * 计算红色方最后一选候选。
     *
     * @param request 阵容、禁用英雄与目标分路等输入
     * @return 推荐结果（通常为 TopN）
     */
    List<LastPickRecommendationDTO> analyzeLastPick(LastPickAnalysisRequestDTO request);
}
