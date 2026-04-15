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

export interface AnalysisResult {
  teamCompAnalysis: string;
  winProbability: {
    blue: number;
    red: number;
  };
  recommendations: string[];
  counters: string[];
}

