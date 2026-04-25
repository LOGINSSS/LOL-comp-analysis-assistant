package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 英雄对位胜率样本实体。
 */
@TableName("champion_matchup")
@Data
public class ChampionMatchup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long championId;     // 英雄 ID
    private Long vsChampionId;   // 对位英雄 ID

    private Double winRate;      // 对位胜率
    private Integer gameCount;   // 样本量
    private String role;         // 对位分路
    private Long lastUpdateTime; // 最后更新时间
}