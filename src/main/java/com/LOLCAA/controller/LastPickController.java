package com.LOLCAA.controller;

import com.LOLCAA.domain.dto.LastPickAnalysisRequestDTO;
import com.LOLCAA.domain.po.DraftAnalysis;
import com.LOLCAA.service.LastPickAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 红色方最后一选分析控制器
 */
@RestController
@RequestMapping("/api/last-pick")
@RequiredArgsConstructor
public class LastPickController {

    private final LastPickAnalysisService lastPickAnalysisService;

    /**
     * 分析红色方最后一选
     */
    @PostMapping("/analyze")
    public DraftAnalysis analyzeLastPick(@RequestBody LastPickAnalysisRequestDTO request) {
        return lastPickAnalysisService.analyzeLastPick(request);
    }
}
