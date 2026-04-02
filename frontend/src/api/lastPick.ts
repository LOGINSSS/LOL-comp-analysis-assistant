import { http } from './http';
import type { DraftAnalysisResponse, LastPickAnalysisRequest } from '@/types/lastPick';

export async function analyzeLastPick(payload: LastPickAnalysisRequest) {
  const response = await http.post<DraftAnalysisResponse>('/last-pick/analyze', payload);
  return response.data;
}

