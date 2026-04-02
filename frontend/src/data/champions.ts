import type { ChampionOption } from '@/types/lastPick';

export const roleOrder = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUP'] as const;
export type RoleKey = (typeof roleOrder)[number];

export const roleNameMap: Record<RoleKey, string> = {
  TOP: '上单',
  JUNGLE: '打野',
  MID: '中单',
  ADC: '下路',
  SUP: '辅助'
};

export const championPool: ChampionOption[] = [
  { id: 266, name: 'Aatrox', primaryRole: 'TOP', secondaryRole: 'MID' },
  { id: 103, name: 'Ahri', primaryRole: 'MID', secondaryRole: 'SUP' },
  { id: 84, name: 'Akali', primaryRole: 'MID', secondaryRole: 'TOP' },
  { id: 12, name: 'Alistar', primaryRole: 'SUP', secondaryRole: 'TOP' },
  { id: 32, name: 'Amumu', primaryRole: 'JUNGLE', secondaryRole: 'SUP' },
  { id: 34, name: 'Anivia', primaryRole: 'MID', secondaryRole: 'SUP' },
  { id: 1, name: 'Annie', primaryRole: 'MID', secondaryRole: 'SUP' },
  { id: 22, name: 'Ashe', primaryRole: 'ADC', secondaryRole: 'SUP' },
  { id: 25, name: 'Morgana', primaryRole: 'SUP', secondaryRole: 'MID' },
  { id: 51, name: 'Caitlyn', primaryRole: 'ADC', secondaryRole: 'MID' },
  { id: 54, name: 'Malphite', primaryRole: 'TOP', secondaryRole: 'SUP' },
  { id: 58, name: 'Renekton', primaryRole: 'TOP', secondaryRole: 'MID' },
  { id: 64, name: 'Lee Sin', primaryRole: 'JUNGLE', secondaryRole: 'TOP' },
  { id: 67, name: 'Vayne', primaryRole: 'ADC', secondaryRole: 'TOP' },
  { id: 69, name: 'Cassiopeia', primaryRole: 'MID', secondaryRole: 'TOP' },
  { id: 72, name: 'Skarner', primaryRole: 'JUNGLE', secondaryRole: 'TOP' },
  { id: 79, name: 'Gragas', primaryRole: 'JUNGLE', secondaryRole: 'SUP' },
  { id: 81, name: 'Ezreal', primaryRole: 'ADC', secondaryRole: 'MID' },
  { id: 99, name: 'Lux', primaryRole: 'MID', secondaryRole: 'SUP' },
  { id: 110, name: 'Varus', primaryRole: 'ADC', secondaryRole: 'MID' },
  { id: 111, name: 'Nautilus', primaryRole: 'SUP', secondaryRole: 'JUNGLE' },
  { id: 126, name: 'Jayce', primaryRole: 'TOP', secondaryRole: 'MID' },
  { id: 131, name: 'Diana', primaryRole: 'MID', secondaryRole: 'JUNGLE' },
  { id: 133, name: 'Quinn', primaryRole: 'TOP', secondaryRole: 'ADC' },
  { id: 150, name: 'Gnar', primaryRole: 'TOP', secondaryRole: 'MID' },
  { id: 157, name: 'Yasuo', primaryRole: 'MID', secondaryRole: 'TOP' },
  { id: 161, name: 'Velkoz', primaryRole: 'MID', secondaryRole: 'SUP' },
  { id: 163, name: 'Taliyah', primaryRole: 'MID', secondaryRole: 'JUNGLE' },
  { id: 201, name: 'Braum', primaryRole: 'SUP', secondaryRole: 'TOP' },
  { id: 222, name: 'Jinx', primaryRole: 'ADC', secondaryRole: 'MID' },
  { id: 235, name: 'Senna', primaryRole: 'SUP', secondaryRole: 'ADC' },
  { id: 236, name: 'Lucian', primaryRole: 'ADC', secondaryRole: 'MID' },
  { id: 238, name: 'Zed', primaryRole: 'MID', secondaryRole: 'JUNGLE' },
  { id: 245, name: 'Ekko', primaryRole: 'MID', secondaryRole: 'JUNGLE' },
  { id: 254, name: 'Vi', primaryRole: 'JUNGLE', secondaryRole: 'TOP' },
  { id: 350, name: 'Yuumi', primaryRole: 'SUP', secondaryRole: 'MID' },
  { id: 412, name: 'Thresh', primaryRole: 'SUP', secondaryRole: 'TOP' },
  { id: 420, name: 'Illaoi', primaryRole: 'TOP', secondaryRole: 'JUNGLE' },
  { id: 497, name: 'Rakan', primaryRole: 'SUP', secondaryRole: 'MID' },
  { id: 498, name: 'Xayah', primaryRole: 'ADC', secondaryRole: 'SUP' },
  { id: 517, name: 'Sylas', primaryRole: 'MID', secondaryRole: 'JUNGLE' },
  { id: 518, name: 'Neeko', primaryRole: 'MID', secondaryRole: 'SUP' },
  { id: 523, name: 'Aphelios', primaryRole: 'ADC', secondaryRole: 'MID' },
  { id: 555, name: 'Pyke', primaryRole: 'SUP', secondaryRole: 'MID' },
  { id: 777, name: 'Yone', primaryRole: 'MID', secondaryRole: 'TOP' },
  { id: 876, name: 'Lillia', primaryRole: 'JUNGLE', secondaryRole: 'MID' },
  { id: 895, name: 'Nilah', primaryRole: 'ADC', secondaryRole: 'SUP' },
  { id: 901, name: 'Smolder', primaryRole: 'ADC', secondaryRole: 'MID' },
  { id: 910, name: 'Hwei', primaryRole: 'MID', secondaryRole: 'SUP' }
];

