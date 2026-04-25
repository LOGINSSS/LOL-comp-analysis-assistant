package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 英雄能力画像实体。
 */
@TableName("champion_stat_profile")
@Data
public class ChampionStatProfile {
    @TableId(type = IdType.INPUT) // 使用 champion_id 作为主键
    private Long championId;

    // 生存/前排能力
    private Integer tankiness;
    private Integer frontline;

    // 输出能力
    private Integer burst;
    private Integer dps;
    private Integer poke;

    // 控制/开团/保护能力
    private Integer cc;
    private Integer engage;
    private Integer disengage;
    private Integer peel;

    // 辅助能力
    private Integer heal;
    private Integer shield;
    private Integer buff;
    private Integer debuff;

    // 刺客能力
    private Integer assassination;
    private Integer mobility;
    private Integer backlineAccess;
}
