<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import ChampionCard from './ChampionCard.vue';
import { roleLabels } from '@/data/roles';
import type { Champion, RoleCode } from '@/types/draft';

interface Props {
  champions: Champion[];
  selectedRole: RoleCode | 'ALL';
  keyword: string;
  bannedIds: number[];
  bluePicked: number[];
  redPicked: number[];
  selectedChampionId: number | null;
  sortMode: 'ID' | 'NAME' | 'ROLE' | 'STATE';
}

const props = defineProps<Props>();
const poolRef = ref<HTMLElement | null>(null);

const emit = defineEmits<{
  'update:selectedRole': [value: RoleCode | 'ALL'];
  'update:keyword': [value: string];
  'update:sortMode': [value: 'ID' | 'NAME' | 'ROLE' | 'STATE'];
  select: [championId: number];
}>();

const roleRank: Record<RoleCode, number> = {
  TOP: 0,
  JUNGLE: 1,
  MID: 2,
  ADC: 3,
  SUP: 4
};

const stateRank: Record<ReturnType<typeof cardState>, number> = {
  available: 0,
  'picked-blue': 1,
  'picked-red': 1,
  banned: 2
};

const roleTabs: Array<{ key: RoleCode | 'ALL'; label: string }> = [
  { key: 'ALL', label: '全部' },
  { key: 'TOP', label: roleLabels.TOP },
  { key: 'JUNGLE', label: roleLabels.JUNGLE },
  { key: 'MID', label: roleLabels.MID },
  { key: 'ADC', label: roleLabels.ADC },
  { key: 'SUP', label: roleLabels.SUP }
];

const filtered = computed(() => {
  const keyword = props.keyword.trim().toLowerCase();
  const sorted = props.champions
    .map((champion) => ({
      champion,
      state: cardState(champion.id)
    }))
    .filter(({ champion }) => {
      if (props.selectedRole === 'ALL') return true;
      return champion.primaryRole === props.selectedRole || champion.secondaryRole === props.selectedRole;
    })
    .filter(({ champion }) => (keyword ? champion.name.toLowerCase().includes(keyword) : true));

  sorted.sort((left, right) => {
    if (props.sortMode === 'NAME') {
      return left.champion.name.localeCompare(right.champion.name, 'zh-Hans-CN');
    }
    if (props.sortMode === 'ROLE') {
      const roleDelta = roleRank[left.champion.primaryRole] - roleRank[right.champion.primaryRole];
      if (roleDelta !== 0) return roleDelta;
      return left.champion.id - right.champion.id;
    }
    if (props.sortMode === 'STATE') {
      const stateDelta = stateRank[left.state] - stateRank[right.state];
      if (stateDelta !== 0) return stateDelta;
      return left.champion.id - right.champion.id;
    }
    return left.champion.id - right.champion.id;
  });

  return sorted.map((item) => item.champion);
});

function cardState(championId: number): 'available' | 'banned' | 'picked-blue' | 'picked-red' {
  if (props.bannedIds.includes(championId)) return 'banned';
  if (props.bluePicked.includes(championId)) return 'picked-blue';
  if (props.redPicked.includes(championId)) return 'picked-red';
  return 'available';
}

function selectChampion(championId: number) {
  emit('select', championId);
}

function changeRole(role: RoleCode | 'ALL') {
  emit('update:selectedRole', role);
}

function changeSortMode(value: 'ID' | 'NAME' | 'ROLE' | 'STATE') {
  emit('update:sortMode', value);
}

function resetPoolScroll() {
  if (!poolRef.value) return;
  poolRef.value.scrollTop = 0;
}

function handlePoolKeydown(event: KeyboardEvent) {
  if (!poolRef.value) return;
  const pageOffset = Math.max(poolRef.value.clientHeight - 32, 180);
  if (event.key === 'Home') {
    poolRef.value.scrollTo({ top: 0, behavior: 'smooth' });
    event.preventDefault();
    return;
  }
  if (event.key === 'End') {
    poolRef.value.scrollTo({ top: poolRef.value.scrollHeight, behavior: 'smooth' });
    event.preventDefault();
    return;
  }
  if (event.key === 'PageDown') {
    poolRef.value.scrollBy({ top: pageOffset, behavior: 'smooth' });
    event.preventDefault();
    return;
  }
  if (event.key === 'PageUp') {
    poolRef.value.scrollBy({ top: -pageOffset, behavior: 'smooth' });
    event.preventDefault();
  }
}

watch([() => props.selectedRole, () => props.keyword], resetPoolScroll);
</script>

<template>
  <section class="panel hero-pool-panel flex flex-col">
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <button
        v-for="tab in roleTabs"
        :key="tab.key"
        type="button"
        class="tab-btn"
        :class="selectedRole === tab.key ? 'tab-btn-active' : ''"
        @click="changeRole(tab.key)"
      >
        {{ tab.label }}
      </button>
      <input
        :value="keyword"
        class="search-input ml-auto"
        placeholder="搜索英雄名称"
        @input="emit('update:keyword', ($event.target as HTMLInputElement).value)"
      />
    </div>

    <div ref="poolRef" class="hero-pool-scroll flex-1" tabindex="0" @keydown="handlePoolKeydown">
      <div class="hero-pool-grid">
        <ChampionCard
          v-for="champion in filtered"
          :key="champion.id"
          :champion="champion"
          :state="cardState(champion.id)"
          :selected="selectedChampionId === champion.id"
          @pick="selectChampion"
        />
      </div>
    </div>

    <div class="pool-sort-bar mt-3">
      <span class="pool-sort-label">排序模式</span>
      <select
        class="pool-sort-select"
        :value="sortMode"
        @change="changeSortMode(($event.target as HTMLSelectElement).value as 'ID' | 'NAME' | 'ROLE' | 'STATE')"
      >
        <option value="ID">默认(ID)</option>
        <option value="NAME">名称(A-Z)</option>
        <option value="ROLE">分路优先</option>
        <option value="STATE">状态优先(可选>已选>已禁)</option>
      </select>
    </div>
  </section>
</template>


