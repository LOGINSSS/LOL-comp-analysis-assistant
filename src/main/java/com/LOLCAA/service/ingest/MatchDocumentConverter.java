package com.LOLCAA.service.ingest;

import com.LOLCAA.domain.dto.ingest.MatchDocument;
import com.LOLCAA.domain.dto.ingest.RiotMatchDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Riot 对局 DTO -> ES 文档转换器。
 *
 * 将一场比赛转换为两条“视角文档”（双方各一条），便于后续按阵容条件聚合。
 */
@Component
public class MatchDocumentConverter {

    private static final int RED_TEAM_ID = 100;
    private static final int BLUE_TEAM_ID = 200;

    /**
     * 把 Riot 原始对局数据转换为双方视角文档。
     */
    public List<MatchDocument> toPerspectiveDocuments(RiotMatchDto matchDto) {
        if (matchDto == null || matchDto.getInfo() == null || matchDto.getMetadata() == null) {
            return List.of();
        }

        Map<String, Integer> redMap = buildRoleMap(matchDto, RED_TEAM_ID);
        Map<String, Integer> blueMap = buildRoleMap(matchDto, BLUE_TEAM_ID);

        String matchId = value(matchDto.getMetadata().getMatchId(), "UNKNOWN");

        MatchDocument redDoc = new MatchDocument();
        redDoc.setMatchId(matchId);
        redDoc.setSide("RED");
        redDoc.setWin(teamWin(matchDto, RED_TEAM_ID));
        fillPerspectiveFields(redDoc, redMap, blueMap);

        MatchDocument blueDoc = new MatchDocument();
        blueDoc.setMatchId(matchId);
        blueDoc.setSide("BLUE");
        blueDoc.setWin(teamWin(matchDto, BLUE_TEAM_ID));
        fillPerspectiveFields(blueDoc, blueMap, redMap);

        return List.of(redDoc, blueDoc);
    }

    /**
     * 按“己方/敌方”角色映射填充文档字段。
     */
    private void fillPerspectiveFields(MatchDocument doc, Map<String, Integer> allyMap, Map<String, Integer> enemyMap) {
        doc.setTop(toKeywordChampionId(allyMap.get("TOP")));
        doc.setJungle(toKeywordChampionId(allyMap.get("JUNGLE")));
        doc.setMid(toKeywordChampionId(allyMap.get("MID")));
        doc.setAdc(toKeywordChampionId(allyMap.get("ADC")));
        doc.setSup(toKeywordChampionId(allyMap.get("SUP")));

        doc.setEnemyTop(toKeywordChampionId(enemyMap.get("TOP")));
        doc.setEnemyJungle(toKeywordChampionId(enemyMap.get("JUNGLE")));
        doc.setEnemyMid(toKeywordChampionId(enemyMap.get("MID")));
        doc.setEnemyAdc(toKeywordChampionId(enemyMap.get("ADC")));
        doc.setEnemySup(toKeywordChampionId(enemyMap.get("SUP")));
    }

    /**
     * 从 participants 中提取某一队伍的分路->英雄映射。
     */
    private Map<String, Integer> buildRoleMap(RiotMatchDto matchDto, int teamId) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (matchDto.getInfo().getParticipants() == null) {
            return map;
        }

        for (RiotMatchDto.Participant participant : matchDto.getInfo().getParticipants()) {
            if (participant == null || participant.getTeamId() == null || participant.getChampionId() == null) {
                continue;
            }
            if (participant.getTeamId() != teamId) {
                continue;
            }

            String role = normalizeRole(participant.getTeamPosition(), participant.getIndividualPosition(), participant.getLane());
            if (role != null) {
                map.put(role, participant.getChampionId());
            }
        }
        return map;
    }

    /**
     * 判断某队伍是否获胜。
     */
    private boolean teamWin(RiotMatchDto matchDto, int teamId) {
        if (matchDto.getInfo().getTeams() == null) {
            return false;
        }
        for (RiotMatchDto.Team team : matchDto.getInfo().getTeams()) {
            if (team != null && team.getTeamId() != null && team.getTeamId() == teamId) {
                return Boolean.TRUE.equals(team.getWin());
            }
        }
        return false;
    }

    /**
     * Riot 分路字段存在多个来源，按优先级依次尝试。
     */
    private String normalizeRole(String teamPosition, String individualPosition, String lane) {
        String[] candidates = {teamPosition, individualPosition, lane};
        for (String candidate : candidates) {
            String mapped = mapRole(candidate);
            if (mapped != null) {
                return mapped;
            }
        }
        return null;
    }

    private String mapRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "TOP" -> "TOP";
            case "JUNGLE" -> "JUNGLE";
            case "MIDDLE", "MID" -> "MID";
            case "BOTTOM", "ADC" -> "ADC";
            case "UTILITY", "SUPPORT", "SUP" -> "SUP";
            default -> null;
        };
    }

    /**
     * 字符串为空时返回默认值。
     */
    private String value(String str, String fallback) {
        return (str == null || str.isBlank()) ? fallback : str;
    }

    /**
     * ES 中冠军 ID 使用 keyword，统一转成字符串。
     */
    private String toKeywordChampionId(Integer championId) {
        return championId == null ? null : String.valueOf(championId);
    }
}
