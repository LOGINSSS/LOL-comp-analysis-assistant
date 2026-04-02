package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("draft_analysis")
@Data
public class DraftAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long draftId;  // 外键引用draft表

    // ========== 各权重分数 ==========
    private Double matchupScore;           // 对线权重分 (0-100)
    private String matchupDetail;          // 对线详细分析 (JSON)

    private Double synergyScore;           // 双英雄配合分 (0-100)
    private String synergyDetail;          // 双英雄配合详细 (JSON)

    private Double teamSynergyScore;       // 多英雄联合分 (0-100)
    private String teamSynergyDetail;      // 多英雄联合详细 (JSON)

    private Double allyDestructiveScore;   // 己方毁灭性打击分 (0-100)
    private Double enemyDestructiveScore;  // 敌方毁灭性打击分 (0-100)

    // ========== 最终结果 ==========
    private Double finalScore;             // 综合评分 (-100 ~ 100)
    private String recommendation;         // 推荐（例如："红方劣势"、"蓝方小优"）
    private String winProbability;         // 预估胜率

    private Long createTime;
    private Long analysisTime;  // 分析耗时（毫秒）
}