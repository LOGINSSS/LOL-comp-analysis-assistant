package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("champion_synergy")
@Data
public class ChampionSynergy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long champion1Id;  // 英雄1 ID
    private Long champion2Id;  // 英雄2 ID

    private Double winRate;      // 双英雄配合胜率
    private Integer gameCount;   // 数据样本量
    private String source;       // "op.gg" 或 "custom"
    private Long lastUpdateTime;
}