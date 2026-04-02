package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("champion")
@Data
public class Champion {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String primaryRole;    // 主要位置
    private String secondaryRole;  // 次要位置
    private String tier;           // 段位评级
    private Double pickRate;       // 选取率
    private Double winRate;        // 胜率
}