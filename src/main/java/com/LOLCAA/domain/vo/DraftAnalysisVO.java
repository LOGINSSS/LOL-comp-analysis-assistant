package com.LOLCAA.domain.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

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