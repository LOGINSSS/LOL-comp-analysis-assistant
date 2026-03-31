package com.LOLCAA.domain.po;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "champion_matchup")
@Data
public class ChampionMatchup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "champion_id")
    private Champion champion;  // 这个英雄

    @ManyToOne
    @JoinColumn(name = "vs_champion_id")
    private Champion vsChampion;  // 对线对手

    private Double winRate;      // 对线胜率（权重）
    private Integer gameCount;   // 对线数据样本量
    private String role;         // 对线位置
    private Long lastUpdateTime; // 最后更新时间
}