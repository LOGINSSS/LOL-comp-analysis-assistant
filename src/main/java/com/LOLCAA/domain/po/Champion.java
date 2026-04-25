package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 英雄基础信息实体。
 */
@TableName("champion")
@Data
public class Champion {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String imageUrl;

    private String primaryRole;    // 主分路
    private String secondaryRole;  // 副分路
    private String tier;           // 强度评级
    private Double pickRate;       // 选取率
    private Double winRate;        // 胜率
}