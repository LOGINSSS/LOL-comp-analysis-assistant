export interface LastPickAnalysisRequest {
  bannedChampions: number[];
  allyPickedChampions: number[];
  enemyPickedChampions: number[];
  requiredRole: string;
}

export interface DraftAnalysisResponse {
  id?: number;
  draftId?: number;
  matchupScore?: number;
  matchupDetail?: string;
  synergyScore?: number;
  synergyDetail?: string;
  teamSynergyScore?: number;
  teamSynergyDetail?: string;
  allyDestructiveScore?: number;
  enemyDestructiveScore?: number;
  finalScore?: number;
  recommendation?: string;
  winProbability?: string;
  createTime?: number;
  analysisTime?: number;
}

export type ChampionRoleKey = 'TOP' | 'JUNGLE' | 'MID' | 'ADC' | 'SUP';

export interface ChampionAttributeScores {
  tankiness?: number;
  frontline?: number;
  burst?: number;
  dps?: number;
  poke?: number;
  cc?: number;
  engage?: number;
  disengage?: number;
  peel?: number;
  heal?: number;
  shield?: number;
  buff?: number;
  debuff?: number;
  assassination?: number;
  mobility?: number;
  backlineAccess?: number;
}

export interface ChampionOption {
  id: number;
  name: string;
  primaryRole: string;
  secondaryRole?: string;
  tier?: string;
  pickRate?: number;
  winRate?: number;
  imageUrl?: string;
  attributes?: ChampionAttributeScores;
}

export type ChampionBuckets = Record<ChampionRoleKey, ChampionOption[]>;

