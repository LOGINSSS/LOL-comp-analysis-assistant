import type { RoleCode } from '@/types/draft';

export const roleLabels: Record<RoleCode, string> = {
  TOP: '上单',
  JUNGLE: '打野',
  MID: '中单',
  ADC: '射手',
  SUP: '辅助'
};

export const roleOrder: RoleCode[] = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUP'];

