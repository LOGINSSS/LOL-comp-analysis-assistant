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

export interface ChampionOption {
  id: number;
  name: string;
  primaryRole: string;
  secondaryRole?: string;
}

