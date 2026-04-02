package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("draft")
@Data
public class Draft {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 己方5个英雄 ID，逗号分隔
    private String allyTeamIds;

    // 敌方5个英雄 ID，逗号分隔
    private String enemyTeamIds;

    private Long createTime;
    private String region;  // 地区标识
}