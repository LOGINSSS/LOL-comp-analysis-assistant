<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import AnalysisResult from '@/components/AnalysisResult.vue';
import DraftActionBar from '@/components/DraftActionBar.vue';
import DraftHeader from '@/components/DraftHeader.vue';
import HeroPool from '@/components/HeroPool.vue';
import TeamPanel from '@/components/TeamPanel.vue';
import { fetchChampions } from '@/api/champions';
import { submitDraft } from '@/api/draft';
import { roleLabels, roleOrder } from '@/data/roles';
import type { AnalysisResult as AnalysisResultType, Champion, RoleCode, Side } from '@/types/draft';

type DraftPhase = 'BAN' | 'PICK' | 'FINAL_PICK' | 'DONE';
type SortMode = 'ID' | 'NAME' | 'ROLE' | 'STATE';
type ActionMode = 'ban' | 'pick' | 'analyze';
const allRoles: RoleCode[] = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUP'];

const banSequence = [0, 5, 1, 6, 2, 7, 3, 8, 4, 9] as const;

const selectedRole = ref<RoleCode | 'ALL'>('ALL');
const keyword = ref('');
const sortMode = ref<SortMode>('ID');
const selectedChampionId = ref<number | null>(null);
const banStep = ref(0);
const finalPickLocked = ref(false);
const loading = ref(false);
const result = ref<AnalysisResultType | null>(null);
const statusText = ref('BAN 阶段：请按顺序禁用英雄（蓝1 -> 红1 -> 蓝2 -> 红2 ...）');
const champions = ref<Champion[]>([]);
const activePickTarget = ref<{ side: Side; role: RoleCode } | null>(null);

const bans = ref<Array<number | null>>(Array.from({ length: 10 }, () => null));

const blueTeam = reactive<Record<RoleCode, number | null>>({ TOP: null, JUNGLE: null, MID: null, ADC: null, SUP: null });
const redTeam = reactive<Record<RoleCode, number | null>>({ TOP: null, JUNGLE: null, MID: null, ADC: null, SUP: null });

const championMap = computed(() => new Map(champions.value.map((champion) => [champion.id, champion])));
const selectedChampion = computed<Champion | null>(() => {
  if (selectedChampionId.value == null) return null;
  return championMap.value.get(selectedChampionId.value) ?? null;
});

const bluePicked = computed<number[]>(() => Object.values(blueTeam).flatMap((id) => (id == null ? [] : [id])));
const redPicked = computed<number[]>(() => Object.values(redTeam).flatMap((id) => (id == null ? [] : [id])));

const bluePickCount = computed(() => bluePicked.value.length);
const redPickCount = computed(() => redPicked.value.length);
const picksReadyForAnalysis = computed(() => bluePickCount.value === 5 && redPickCount.value === 4);
const remainingRedRoles = computed<RoleCode[]>(() => roleOrder.filter((role) => redTeam[role] == null));
const redFinalRole = computed<RoleCode>(() => remainingRedRoles.value[0] ?? 'SUP');

const phase = computed<DraftPhase>(() => {
  if (finalPickLocked.value) return 'DONE';
  if (banStep.value < banSequence.length) return 'BAN';
  if (!picksReadyForAnalysis.value) return 'PICK';
  return 'FINAL_PICK';
});

const activeBanIndex = computed(() => (phase.value === 'BAN' ? banSequence[banStep.value] : -1));

const phaseText = computed(() => {
  if (phase.value === 'BAN') return `BAN PHASE ${banStep.value + 1}/${banSequence.length}`;
  if (phase.value === 'PICK') return `PICK PHASE 蓝${bluePickCount.value}/5 红${redPickCount.value}/4`;
  if (phase.value === 'FINAL_PICK') return `READY TO ANALYZE (${roleLabels[redFinalRole.value]}位待分析)`;
  return 'ANALYSIS COMPLETE';
});

const blueActiveRole = computed(() => (phase.value === 'PICK' && activePickTarget.value?.side === 'blue' ? activePickTarget.value.role : null));
const redActiveRole = computed(() => (phase.value === 'PICK' && activePickTarget.value?.side === 'red' ? activePickTarget.value.role : null));
const redDisabledRoles = computed<RoleCode[]>(() => {
  if (phase.value !== 'PICK') return [];
  if (redPickCount.value < 4) return [];
  return remainingRedRoles.value;
});

const roleShortLabels: Record<RoleCode, string> = {
  TOP: '上',
  JUNGLE: '野',
  MID: '中',
  ADC: '下',
  SUP: '辅'
};

const actionMode = computed<ActionMode>(() => {
  if (phase.value === 'BAN') return 'ban';
  if (phase.value === 'PICK') return 'pick';
  return 'analyze';
});

const confirmLabel = computed(() => {
  if (phase.value === 'BAN') return `确认禁用 ${selectedChampion.value?.name ?? '英雄名'}`;
  if (phase.value === 'PICK') {
    const slotText = activePickTarget.value
      ? `${activePickTarget.value.side === 'blue' ? '蓝方' : '红方'}${roleShortLabels[activePickTarget.value.role]}`
      : '未选位置';
    return `确认选择 ${selectedChampion.value?.name ?? '英雄名'}（${slotText}）`;
  }
  return '确认开始分析';
});

const lockEnabled = computed(() => {
  if (loading.value || phase.value === 'DONE') return false;
  if (phase.value === 'FINAL_PICK') return true;
  if (!selectedChampion.value) return false;
  if (isUnavailable(selectedChampion.value.id)) return false;
  if (phase.value === 'PICK') {
    if (!activePickTarget.value) return false;
    return isPickTargetSelectable(activePickTarget.value.side, activePickTarget.value.role);
  }
  return true;
});

function isUnavailable(championId: number) {
  return bans.value.includes(championId) || bluePicked.value.includes(championId) || redPicked.value.includes(championId);
}

function championName(championId: number) {
  return championMap.value.get(championId)?.name ?? `#${championId}`;
}

function currentBanSide(index: number): 'blue' | 'red' {
  return index < 5 ? 'blue' : 'red';
}

function currentBanLocalSlot(index: number): number {
  return index < 5 ? index + 1 : index - 4;
}

function isPickTargetSelectable(side: Side, role: RoleCode) {
  if (phase.value !== 'PICK') return false;
  const team = side === 'blue' ? blueTeam : redTeam;
  if (team[role] != null) return false;
  if (side === 'blue' && bluePickCount.value >= 5) return false;
  if (side === 'red' && redPickCount.value >= 4) return false;
  return true;
}

function selectChampion(championId: number) {
  selectedChampionId.value = championId;
  if (isUnavailable(championId)) {
    statusText.value = '该英雄已被 Ban 或已被选择';
    return;
  }
  statusText.value = `已选中 ${championName(championId)}，点击按钮确认。`;
}

function setActiveSlot(side: Side, role: RoleCode) {
  if (phase.value !== 'PICK') return;
  if (!isPickTargetSelectable(side, role)) {
    if (side === 'red' && redPickCount.value >= 4) {
      statusText.value = '红方已完成 4 手选择，剩余位置已锁定用于分析。';
      return;
    }
    statusText.value = '该位置当前不可选，请选择空位置。';
    return;
  }
  activePickTarget.value = { side, role };
  statusText.value = `已选择 ${side === 'blue' ? '蓝方' : '红方'} ${roleLabels[role]} 位置，等待确认英雄。`;
}

function finalizeBan(championId: number) {
  const banIndex = activeBanIndex.value;
  if (banIndex < 0) return;
  bans.value[banIndex] = championId;
  const side = currentBanSide(banIndex);
  const localSlot = currentBanLocalSlot(banIndex);
  banStep.value += 1;
  statusText.value = `${side === 'blue' ? '蓝方' : '红方'}第 ${localSlot} Ban 锁定 ${championName(championId)}`;
}

function finalizePick(championId: number) {
  if (!activePickTarget.value) {
    statusText.value = '请先点击要选择的位置，再确认英雄。';
    return;
  }
  const { side, role } = activePickTarget.value;
  if (!isPickTargetSelectable(side, role)) {
    statusText.value = '该位置已不可选，请重新点击有效位置。';
    return;
  }
  const team = side === 'blue' ? blueTeam : redTeam;
  team[role] = championId;
  statusText.value = `${side === 'blue' ? '蓝方' : '红方'} ${roleLabels[role]} 锁定 ${championName(championId)}`;
  activePickTarget.value = null;
}

async function submitAnalysis() {
  loading.value = true;
  statusText.value = '正在提交阵容并等待分析结果...';

  try {
    result.value = await submitDraft({
      bans: bans.value.filter((id): id is number => id != null),
      blueTeam: { ...blueTeam },
      redTeam: { ...redTeam },
      finalPickRole: redFinalRole.value
    });
    finalPickLocked.value = true;
    statusText.value = '分析完成';
  } catch {
    statusText.value = '分析请求失败，请检查后端接口 /last-pick/analyze';
  } finally {
    loading.value = false;
  }
}

async function lockInActive() {
  if (phase.value === 'FINAL_PICK') {
    await submitAnalysis();
    return;
  }

  if (!selectedChampion.value) {
    statusText.value = '请先在英雄池中选择一个英雄';
    return;
  }

  if (isUnavailable(selectedChampion.value.id)) {
    statusText.value = '该英雄已被 Ban 或已被选择';
    return;
  }

  if (phase.value === 'BAN') {
    finalizeBan(selectedChampion.value.id);
  } else if (phase.value === 'PICK') {
    finalizePick(selectedChampion.value.id);
  }

  selectedChampionId.value = null;
}

async function loadChampions() {
  try {
    // Load each role explicitly to avoid backend defaults that may return only one lane.
    const responses = await Promise.all(allRoles.map((role) => fetchChampions({ role })));
    const merged = new Map<number, Champion>();
    for (const list of responses) {
      for (const champion of list) {
        if (!merged.has(champion.id)) merged.set(champion.id, champion);
      }
    }
    champions.value = Array.from(merged.values()).sort((a, b) => a.id - b.id);
  } catch {
    champions.value = [];
    statusText.value = '英雄列表加载失败，请检查后端接口 /champions';
  }
}


onMounted(() => {
  loadChampions();
});
</script>

<template>
  <div class="draft-root">
    <div class="draft-shell">
      <DraftHeader :phase-text="phaseText" :bans="bans" :champion-map="championMap" :active-ban-index="activeBanIndex" />

      <main class="draft-main">
        <TeamPanel
          side="blue"
          :team="blueTeam"
          :champion-map="championMap"
          :active-role="blueActiveRole"
          @select-slot="(role) => setActiveSlot('blue', role)"
        />

        <div class="space-y-4">
          <HeroPool
            :champions="champions"
            :selected-role="selectedRole"
            :keyword="keyword"
            :sort-mode="sortMode"
            :banned-ids="bans.filter((id): id is number => id != null)"
            :blue-picked="bluePicked"
            :red-picked="redPicked"
            :selected-champion-id="selectedChampionId"
            @update:selected-role="selectedRole = $event"
            @update:keyword="keyword = $event"
            @update:sort-mode="sortMode = $event"
            @select="selectChampion"
          />
        </div>

        <TeamPanel
          side="red"
          :team="redTeam"
          :champion-map="championMap"
          :active-role="redActiveRole"
          :disabled-roles="redDisabledRoles"
          @select-slot="(role) => setActiveSlot('red', role)"
        />
      </main>

      <div class="px-4 pb-4">
        <DraftActionBar
          :status-text="statusText"
          :lock-enabled="lockEnabled"
          :loading="loading"
          :confirm-label="confirmLabel"
          :action-mode="actionMode"
          @lock="lockInActive"
        />
        <AnalysisResult :result="result" :loading="loading" />
      </div>
    </div>
  </div>
</template>

