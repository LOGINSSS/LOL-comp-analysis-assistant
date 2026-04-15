import { http } from './http';
import type { Champion, RoleCode } from '@/types/draft';

interface ChampionQuery {
  role?: RoleCode | 'ALL';
  name?: string;
}

export async function fetchChampions(query: ChampionQuery = {}): Promise<Champion[]> {
  const params: Record<string, string> = {};
  if (query.role && query.role !== 'ALL') {
	params.role = query.role;
  }
  if (query.name && query.name.trim()) {
	params.name = query.name.trim();
  }

  const response = await http.get<Champion[]>('/champions', { params });
  return response.data;
}

