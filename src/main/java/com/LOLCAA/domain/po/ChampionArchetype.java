package com.LOLCAA.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("champion_archetype")
@Data
public class ChampionArchetype {
    @TableId(type = IdType.INPUT) // 使用外部输入的ID，因为它是champion_id
    private Long championId;

    private String macroRole;  // FRONTLINE / BACKLINE / ASSASSIN
    private String subRole;    // TANK / DPS / POKE / ENGAGE 等
}
