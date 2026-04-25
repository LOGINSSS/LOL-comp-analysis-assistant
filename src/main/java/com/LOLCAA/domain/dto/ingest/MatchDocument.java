package com.LOLCAA.domain.dto.ingest;

import lombok.Data;

/**
 * ES `lol_matches` 索引文档模型。
 *
 * 一场比赛会产生两条记录：RED 视角一条、BLUE 视角一条。
 */
@Data
public class MatchDocument {
	private String matchId;
	private String side;
	private Boolean win;

	// 自方五个位置
	private String top;
	private String jungle;
	private String mid;
	private String adc;
	private String sup;

	// 敌方五个位置
	private String enemyTop;
	private String enemyJungle;
	private String enemyMid;
	private String enemyAdc;
	private String enemySup;
}
