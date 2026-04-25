package com.LOLCAA.service.impl;

import com.LOLCAA.domain.dto.ChampionListItemDTO;
import com.LOLCAA.domain.po.Champion;
import com.LOLCAA.mapper.ChampionMapper;
import com.LOLCAA.service.ChampionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 英雄查询服务实现。
 *
 * 职责：
 * 1) 规范化前端传入的 role/name；
 * 2) 调用 Mapper 执行查询；
 * 3) 将数据库实体转换为前端展示 DTO。
 */
@Service
@RequiredArgsConstructor
public class ChampionQueryServiceImpl implements ChampionQueryService {

    /**
     * 默认分路（当前用于空 role 或未知 role 的兼容回退）。
     */
    private static final String DEFAULT_ROLE = "TOP";

    private final ChampionMapper championMapper;

    /**
     * 英雄列表查询入口。
     *
     * @param role 前端传入分路（可空，支持别名）
     * @param name 前端传入名称关键词（可空）
     * @return 适配前端字段格式的英雄列表
     */
    @Override
    public List<ChampionListItemDTO> listChampions(String role, String name) {
        // 名称只做基础去空格，具体模糊匹配交给 SQL 处理。
        String normalizedName = StringUtils.hasText(name) ? name.trim() : null;
        List<String> roles = toRoleAliases(role);

        return championMapper.findByRoleAndName(roles, normalizedName).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 将前端 role 归一化为数据库可查询的分路别名集合。
     */
    private List<String> toRoleAliases(String role) {
        // 空值或 ALL 走默认回退，保持当前既有行为。
        if (!StringUtils.hasText(role) || "ALL".equalsIgnoreCase(role)) {
            return List.of(DEFAULT_ROLE);
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TOP" -> List.of("TOP");
            // 兼容前端/历史数据的打野写法差异。
            case "JG", "JUNGLE" -> List.of("JG", "JUNGLE");
            case "MID" -> List.of("MID");
            case "ADC" -> List.of("ADC");
            // 兼容辅助写法差异。
            case "SUP", "SUPPORT" -> List.of("SUP", "SUPPORT");
            // 未知分路统一回退，避免直接返回空结果。
            default -> List.of(DEFAULT_ROLE);
        };
    }

    /**
     * 数据库实体 -> 前端列表项 DTO。
     */
    private ChampionListItemDTO toDto(Champion champion) {
        return new ChampionListItemDTO(
                champion.getId(),
                champion.getName(),
                champion.getImageUrl(),
                normalizeRoleForFrontend(champion.getPrimaryRole()),
                normalizeRoleForFrontend(champion.getSecondaryRole())
        );
    }

    /**
     * 将数据库分路编码转换为前端统一展示格式。
     */
    private String normalizeRoleForFrontend(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if ("JG".equals(normalized)) {
            return "JUNGLE";
        }
        if ("SUPPORT".equals(normalized)) {
            return "SUP";
        }
        return normalized;
    }
}
