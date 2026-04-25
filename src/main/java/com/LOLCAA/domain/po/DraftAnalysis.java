package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 阵容分析结果实体。
 */
@TableName("draft_analysis")
@Data
public class DraftAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long draftId;  // 关联 draft.id

    // 各维度评分
    private Double matchupScore;           // 对线评分 (0-100)
    private String matchupDetail;          // 对线详情 (JSON)

    private Double synergyScore;           // 双英雄协同评分 (0-100)
    private String synergyDetail;          // 双英雄协同详情 (JSON)

    private Double teamSynergyScore;       // 阵容协同评分 (0-100)
    private String teamSynergyDetail;      // 阵容协同详情 (JSON)

    private Double allyDestructiveScore;   // 己方克制度评分 (0-100)
    private Double enemyDestructiveScore;  // 敌方克制度评分 (0-100)

    // 综合结果
    private Double finalScore;
    private String recommendation;
    private String winProbability;

    private Long createTime;
    private Long analysisTime;  // 分析耗时（毫秒）
}