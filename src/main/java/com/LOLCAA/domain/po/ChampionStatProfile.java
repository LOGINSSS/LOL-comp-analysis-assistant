package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("champion_stat_profile")
@Data
public class ChampionStatProfile {
    @TableId(type = IdType.INPUT) // 使用外部输入的ID，因为它是champion_id
    private Long championId;

    // 🛡️ 生存 / 前排
    private Integer tankiness; // 默认值0
    private Integer frontline; // 默认值0

    // ⚔️ 输出
    private Integer burst; // 默认值0
    private Integer dps;   // 默认值0
    private Integer poke;  // 默认值0

    // 🎯 控制 / 功能
    private Integer cc;       // 默认值0
    private Integer engage;   // 默认值0
    private Integer disengage; // 默认值0
    private Integer peel;      // 默认值0

    // 💊 辅助能力
    private Integer heal;   // 默认值0
    private Integer shield; // 默认值0
    private Integer buff;   // 默认值0
    private Integer debuff; // 默认值0

    // 🗡️ 刺客能力
    private Integer assassination; // 默认值0
    private Integer mobility;      // 默认值0
    private Integer backlineAccess; // 默认值0
}
