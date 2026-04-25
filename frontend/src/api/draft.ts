import { http } from './http';
import type { AnalysisResult, DraftSubmissionRequest, LastPickRecommendation, RoleCode } from '@/types/draft';

interface LastPickAnalyzeRequest {
  bannedChampions: number[];
  allyTop: number | null;
  allyJungle: number | null;
  allyMid: number | null;
  allyAdc: number | null;
  allySup: number | null;
  enemyTop: number | null;
  enemyJungle: number | null;
  enemyMid: number | null;
  enemyAdc: number | null;
  enemySup: number | null;
  requiredRole: RoleCode;
  topN: number;
}

function toLastPickPayload(payload: DraftSubmissionRequest): LastPickAnalyzeRequest {
  return {
    bannedChampions: payload.bans,
    // Last-pick analyze is for red side final pick: ally=red (one slot empty), enemy=blue (fully picked).
    allyTop: payload.redTeam.TOP,
    allyJungle: payload.redTeam.JUNGLE,
    allyMid: payload.redTeam.MID,
    allyAdc: payload.redTeam.ADC,
    allySup: payload.redTeam.SUP,
    enemyTop: payload.blueTeam.TOP,
    enemyJungle: payload.blueTeam.JUNGLE,
    enemyMid: payload.blueTeam.MID,
    enemyAdc: payload.blueTeam.ADC,
    enemySup: payload.blueTeam.SUP,
    requiredRole: payload.finalPickRole,
    topN: 5
  };
}

const stageMeaning: Record<LastPickRecommendation['levelUsed'], string> = {
  P0: '全量阵容强约束命中，策略最严格',
  P1: '己方核心+敌方全量，保持强针对性',
  P2: '己方核心+敌方核心，兼顾样本和针对性',
  P3: '己方核心+目标位对位，强调位置对抗',
  P4: '仅保留目标位对位，优先保证可推荐',
  P5: '基线兜底策略，主要用于低样本场景'
};

function toNumber(value: unknown, fallback = 0): number {
  if (typeof value !== 'number' || Number.isNaN(value)) return fallback;
  return value;
}

function normalizeRecommendation(item: Partial<LastPickRecommendation>): LastPickRecommendation {
  return {
    championId: toNumber(item.championId),
    championName: item.championName?.trim() || `#${toNumber(item.championId)}`,
    winRate: Number(toNumber(item.winRate).toFixed(2)),
    games: Math.max(0, Math.round(toNumber(item.games))),
    levelUsed: (item.levelUsed ?? 'P5') as LastPickRecommendation['levelUsed'],
    evidence: item.evidence?.trim() || '未返回证据描述',
    fallbackReason: item.fallbackReason?.trim() || '',
    confidence: Number(toNumber(item.confidence).toFixed(2))
  };
}

const defaultResult: AnalysisResult = {
  strategySummary: '当前暂无可用样本，建议先补充更多对局后再观察分层策略结果。',
  stageUsed: 'P5',
  topRecommendation: null,
  recommendations: []
};

export async function submitDraft(payload: DraftSubmissionRequest): Promise<AnalysisResult> {
  try {
    const response = await http.post<Partial<LastPickRecommendation>[]>('/last-pick/analyze', toLastPickPayload(payload));
    const normalized = Array.isArray(response.data) ? response.data.map(normalizeRecommendation) : [];
    if (!normalized.length) return defaultResult;

    const topRecommendation = normalized[0];
    const stageDescription = stageMeaning[topRecommendation.levelUsed] ?? stageMeaning.P5;
    return {
      strategySummary: `本次命中 ${topRecommendation.levelUsed} 分层策略：${stageDescription}。Top1 为 ${topRecommendation.championName}（${topRecommendation.winRate}% / ${topRecommendation.games} 场）。`,
      stageUsed: topRecommendation.levelUsed,
      topRecommendation,
      recommendations: normalized
    };
  } catch {
    return defaultResult;
  }
}

