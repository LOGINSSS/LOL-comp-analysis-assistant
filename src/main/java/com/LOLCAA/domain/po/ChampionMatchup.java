package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("champion_matchup")
@Data
public class ChampionMatchup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long championId;     // 英雄 ID
    private Long vsChampionId;   // 对线对手 ID

    private Double winRate;      // 对线胜率（权重）
    private Integer gameCount;   // 对线数据样本量
    private String role;         // 对线位置
    private Long lastUpdateTime; // 最后更新时间
}