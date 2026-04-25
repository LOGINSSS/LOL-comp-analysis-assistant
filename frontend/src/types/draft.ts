export type RoleCode = 'TOP' | 'JUNGLE' | 'MID' | 'ADC' | 'SUP';
export type Side = 'blue' | 'red';

export interface Champion {
  id: number;
  name: string;
  imageUrl: string;
  primaryRole: RoleCode;
  secondaryRole: RoleCode | null;
}

export interface SlotTarget {
  side: Side;
  role: RoleCode;
}

export interface DraftSubmissionRequest {
  bans: number[];
  blueTeam: Record<RoleCode, number | null>;
  redTeam: Record<RoleCode, number | null>;
  finalPickRole: RoleCode;
}

export interface LastPickRecommendation {
  championId: number;
  championName: string;
  winRate: number;
  games: number;
  levelUsed: 'P0' | 'P1' | 'P2' | 'P3' | 'P4' | 'P5';
  evidence: string;
  fallbackReason: string;
  confidence: number;
}

export interface AnalysisResult {
  strategySummary: string;
  stageUsed: LastPickRecommendation['levelUsed'];
  topRecommendation: LastPickRecommendation | null;
  recommendations: LastPickRecommendation[];
}

