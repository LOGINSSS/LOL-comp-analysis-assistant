package com.LOLCAA.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 英雄数据解析工具类
 */
public class HeroParser {

    /**
     * 将逗号分隔的英雄ID字符串解析为List<Long>
     */
    public static List<Long> parseHeroIds(String heroIdsStr) {
        if (heroIdsStr == null || heroIdsStr.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(heroIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * 将List<Long>转换为排序后的逗号分隔字符串（用于Team Synergy匹配）
     */
    public static String formatHeroIdsForSynergy(List<Long> heroIds) {
        return heroIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
