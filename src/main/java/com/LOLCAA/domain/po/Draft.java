package com.LOLCAA.domain.po;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "draft")
@Data
public class Draft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 己方5个英雄 ID，逗号分隔
    @Column(name = "ally_team_ids")
    private String allyTeamIds;

    // 敌方5个英雄 ID，逗号分隔
    @Column(name = "enemy_team_ids")
    private String enemyTeamIds;

    private Long createTime;
    private String region;  // 地区标识
}