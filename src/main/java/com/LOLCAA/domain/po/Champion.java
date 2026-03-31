package com.LOLCAA.domain.po;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "champion")
@Data
public class Champion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    private String role;      // Top/Jungle/Mid/ADC/Support
    private String tier;       // S/A/B/C/D
    private Double pickRate;   // op.gg 选取率
    private Double winRate;    // op.gg 胜率
}