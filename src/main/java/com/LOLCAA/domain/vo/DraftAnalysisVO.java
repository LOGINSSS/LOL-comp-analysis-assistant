package com.LOLCAA.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 阵容分析结果返回 VO。
 */
@Data
public class DraftAnalysisVO {
    @JsonProperty("matchup_score")
    private Double matchupScore;

    @JsonProperty("synergy_score")
    private Double synergyScore;

    @JsonProperty("team_synergy_score")
    private Double teamSynergyScore;

    @JsonProperty("enemy_destructive_score")
    private Double enemyDestructiveScore;

    @JsonProperty("final_score")
    private Double finalScore;

    @JsonProperty("recommendation")
    private String recommendation;

    @JsonProperty("win_probability")
    private String winProbability;
}