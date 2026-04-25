package com.LOLCAA.controller;

import com.LOLCAA.domain.dto.LastPickAnalysisRequestDTO;
import com.LOLCAA.domain.dto.LastPickRecommendationDTO;
import com.LOLCAA.service.LastPickAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 红色方最后一选分析接口。
 */
@RestController
@RequestMapping("/last-pick")
@RequiredArgsConstructor
public class LastPickController {

    private final LastPickAnalysisService lastPickAnalysisService;

    /**
     * 根据当前 BP/阵容信息计算最后一选 TopN 推荐。
     *
     * @param request 最后一选分析请求
     * @return 推荐候选列表（按综合结果排序）
     */
    @PostMapping("/analyze")
    public List<LastPickRecommendationDTO> analyzeLastPick(@RequestBody LastPickAnalysisRequestDTO request) {
        return lastPickAnalysisService.analyzeLastPick(request);
    }
}
