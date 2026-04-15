<script setup lang="ts">
import { computed } from 'vue';
import { roleLabels, roleOrder } from '@/data/roles';
import type { Champion, RoleCode, Side } from '@/types/draft';

interface Props {
  side: Side;
  team: Record<RoleCode, number | null>;
  championMap: Map<number, Champion>;
  activeRole: RoleCode | null;
  disabledRoles?: RoleCode[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  'select-slot': [role: RoleCode];
}>();

const panelTitle = computed(() => (props.side === 'blue' ? '蓝方阵容' : '红方阵容'));

function selectedChampion(role: RoleCode) {
  const id = props.team[role];
  if (typeof id !== 'number') return null;
  return props.championMap.get(id) ?? null;
}

function isRoleDisabled(role: RoleCode) {
  return props.disabledRoles?.includes(role) ?? false;
}
</script>

<template>
  <section class="panel h-full" :class="side === 'blue' ? 'team-blue' : 'team-red'">
	<h3 class="mb-3 text-center text-lg font-semibold">{{ panelTitle }}</h3>
	<div class="space-y-3">
	  <button
		v-for="role in roleOrder"
		:key="`${side}-${role}`"
		type="button"
		class="slot-row"
		:class="activeRole === role ? 'slot-active' : ''"
		:disabled="isRoleDisabled(role)"
		@click="emit('select-slot', role)"
	  >
		<span class="slot-role-label">{{ roleLabels[role] }}</span>
		<span class="slot-avatar-wrap">
		  <span v-if="selectedChampion(role)" class="avatar-frame avatar-slot">
			<img
			  v-motion
			  :initial="{ opacity: 0, y: 10 }"
			  :enter="{ opacity: 1, y: 0 }"
			  class="avatar-img"
			  :src="selectedChampion(role)!.imageUrl"
			  :alt="selectedChampion(role)!.name"
			/>
		  </span>
		  <span v-else class="placeholder-pulse">待选择</span>
		</span>
		<span class="slot-hero-name">{{ selectedChampion(role)?.name ?? '等待选择英雄' }}</span>
	  </button>
	</div>
  </section>
</template>

