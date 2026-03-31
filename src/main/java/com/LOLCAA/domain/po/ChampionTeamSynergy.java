package com.LOLCAA.domain.po;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Set;

@Entity
@Table(name = "champion_team_synergy")
@Data
public class ChampionTeamSynergy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 存储 champion_id 的逗号分隔字符串，便于查询
    // 例如: "1,3,5,7,9" 代表5个英雄的组合
    @Column(name = "champion_ids")
    private String championIds;

    private Double teamSynergyScore;    // 多英雄联合评分（0-100）
    private Integer trainingDataCount;  // 你们的训练数据量
    private String description;         // 组合描述
    private Long lastUpdateTime;
}