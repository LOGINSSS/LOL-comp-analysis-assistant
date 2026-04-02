package com.LOLCAA.utils;

import java.util.List;

/**
 * 评分计算工具类
 */
public class ScoreCalculator {

    /**
     * 计算加权最终分数
     */
    public static double calculateFinalScore(double matchup, double synergy, double teamSynergy, double enemyDestructive) {
        return 0.4 * matchup + 0.2 * synergy + 0.2 * teamSynergy + 0.2 * (100 - enemyDestructive);
    }

    /**
     * 标准化分数到0-100范围
     */
    public static double normalizeScore(double score) {
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 计算平均值
     */
    public static double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(50.0);
    }

    /**
     * 生成胜率描述
     */
    public static String generateWinProbability(double score) {
        if (score > 60) return "优势较大";
        if (score > 55) return "小优";
        if (score > 45) return "均势";
        if (score > 40) return "小劣";
        return "劣势较大";
    }
}
