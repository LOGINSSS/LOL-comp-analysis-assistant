package com.LOLCAA.domain.po;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "champion_synergy")
@Data
public class ChampionSynergy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "champion1_id")
    private Champion champion1;  // 英雄1

    @ManyToOne
    @JoinColumn(name = "champion2_id")
    private Champion champion2;  // 英雄2

    private Double winRate;      // 双英雄配合胜率
    private Integer gameCount;   // 数据样本量
    private String source;       // "op.gg" 或 "custom"
    private Long lastUpdateTime;
}