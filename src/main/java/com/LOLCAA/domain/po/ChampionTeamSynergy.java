package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 多英雄阵容协同实体。
 */
@TableName("champion_team_synergy")
@Data
public class ChampionTeamSynergy {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 排序后的 champion_id 逗号串，例如: "1,3,5,7,9"
    private String championIds;

    private Double teamSynergyScore;    // 多英雄联合评分（0-100）
    private Integer trainingDataCount;  // 训练数据量
    private String description;         // 组合描述
    private Long lastUpdateTime;
}