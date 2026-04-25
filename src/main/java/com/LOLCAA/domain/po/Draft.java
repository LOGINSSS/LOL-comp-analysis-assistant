package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 一次阵容输入（BP 快照）实体。
 */
@TableName("draft")
@Data
public class Draft {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 己方 5 个英雄 ID（逗号分隔）
    private String allyTeamIds;

    // 敌方 5 个英雄 ID（逗号分隔）
    private String enemyTeamIds;

    private Long createTime;
    private String region;  // 区域标识
}