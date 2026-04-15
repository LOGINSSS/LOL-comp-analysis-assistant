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

@Service
@RequiredArgsConstructor
public class ChampionQueryServiceImpl implements ChampionQueryService {

    private static final String DEFAULT_ROLE = "TOP";

    private final ChampionMapper championMapper;

    @Override
    public List<ChampionListItemDTO> listChampions(String role, String name) {
        String normalizedName = StringUtils.hasText(name) ? name.trim() : null;
        List<String> roles = toRoleAliases(role);

        return championMapper.findByRoleAndName(roles, normalizedName).stream()
                .map(this::toDto)
                .toList();
    }

    private List<String> toRoleAliases(String role) {
        if (!StringUtils.hasText(role) || "ALL".equalsIgnoreCase(role)) {
            return List.of(DEFAULT_ROLE);
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TOP" -> List.of("TOP");
            case "JG", "JUNGLE" -> List.of("JG", "JUNGLE");
            case "MID" -> List.of("MID");
            case "ADC" -> List.of("ADC");
            case "SUP", "SUPPORT" -> List.of("SUP", "SUPPORT");
            default -> List.of(DEFAULT_ROLE);
        };
    }

    private ChampionListItemDTO toDto(Champion champion) {
        return new ChampionListItemDTO(
                champion.getId(),
                champion.getName(),
                champion.getImageUrl(),
                normalizeRoleForFrontend(champion.getPrimaryRole()),
                normalizeRoleForFrontend(champion.getSecondaryRole())
        );
    }

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

