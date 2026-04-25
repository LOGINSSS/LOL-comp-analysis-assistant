package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 英雄原型标签实体。
 */
@TableName("champion_archetype")
@Data
public class ChampionArchetype {
    @TableId(type = IdType.INPUT) // 使用 champion_id 作为主键
    private Long championId;

    private String macroRole;  // FRONTLINE / BACKLINE / ASSASSIN
    private String subRole;    // TANK / DPS / POKE / ENGAGE 等
}
