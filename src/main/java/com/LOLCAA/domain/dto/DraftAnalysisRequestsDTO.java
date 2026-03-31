package com.LOLCAA.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class DraftAnalysisRequestsDTO {
    private List<String> allyChampions;    // 己方英雄名 ["Ahri", "Lee Sin", ...]
    private List<String> enemyChampions;   // 敌方英雄名
}