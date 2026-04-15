import { http } from './http';
import type { AnalysisResult, DraftSubmissionRequest, RoleCode } from '@/types/draft';

interface LegacyDraftResponse {
  recommendation?: string;
  winProbability?: string;
  matchupDetail?: string;
  synergyDetail?: string;
  teamSynergyDetail?: string;
}

interface LegacyRequest {
  bannedChampions: number[];
  allyPickedChampions: number[];
  enemyPickedChampions: number[];
  requiredRole: RoleCode;
}

const roleOrder: RoleCode[] = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUP'];

function toLegacyPayload(payload: DraftSubmissionRequest): LegacyRequest {
  return {
	bannedChampions: payload.bans,
	allyPickedChampions: roleOrder.map((role) => payload.blueTeam[role]).filter((id): id is number => typeof id === 'number'),
	enemyPickedChampions: roleOrder.map((role) => payload.redTeam[role]).filter((id): id is number => typeof id === 'number'),
	requiredRole: payload.finalPickRole
  };
}

function parseWinProbability(raw?: string): { blue: number; red: number } {
  if (!raw) return { blue: 50, red: 50 };
  const nums = raw.replace(/%/g, '').match(/\d+(?:\.\d+)?/g) ?? [];
  if (nums.length >= 2) {
	const blue = Number(nums[0]);
	const red = Number(nums[1]);
	if (Number.isFinite(blue) && Number.isFinite(red)) return { blue, red };
  }
  return { blue: 50, red: 50 };
}

const defaultResult: AnalysisResult = {
  teamCompAnalysis: '蓝方偏前中期主动开团，红方后期团战容错更高。',
  winProbability: { blue: 52, red: 48 },
  recommendations: ['红方注意前 10 分钟河道视野', '蓝方可围绕先锋加速滚雪球'],
  counters: ['蓝方上路对线压制明显，红方需优先保发育']
};

export async function submitDraft(payload: DraftSubmissionRequest): Promise<AnalysisResult> {
  try {
	const response = await http.post<LegacyDraftResponse>('/last-pick/analyze', toLegacyPayload(payload));
	const data = response.data;
	return {
	  teamCompAnalysis: data.recommendation ?? defaultResult.teamCompAnalysis,
	  winProbability: parseWinProbability(data.winProbability),
	  recommendations: [data.matchupDetail, data.synergyDetail, data.teamSynergyDetail].filter((x): x is string => Boolean(x)),
	  counters: defaultResult.counters
	};
  } catch {
	return defaultResult;
  }
}

