<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { AnalysisResult as AnalysisResultType } from '@/types/draft';

interface Props {
  result: AnalysisResultType | null;
  loading: boolean;
}

const props = defineProps<Props>();
const expanded = ref(false);
const stageColorMap: Record<string, string> = {
  P0: 'text-emerald-300',
  P1: 'text-cyan-300',
  P2: 'text-sky-300',
  P3: 'text-indigo-300',
  P4: 'text-violet-300',
  P5: 'text-amber-300'
};

const stageColorClass = computed(() => {
  if (!props.result) return stageColorMap.P5;
  return stageColorMap[props.result.stageUsed] ?? stageColorMap.P5;
});

const fallbackSteps = computed(() => {
  const raw = props.result?.topRecommendation?.fallbackReason ?? '';
  return raw
    .split(';')
    .map((step) => step.trim())
    .filter(Boolean);
});

watch(
  () => props.result,
  (value) => {
    if (value) expanded.value = true;
  }
);
</script>

<template>
  <section class="analysis-dock panel mt-3">
    <button type="button" class="analysis-dock-title" @click="expanded = !expanded">
      分析结果 {{ expanded ? '▲' : '▼' }}
    </button>

    <div v-if="loading" class="space-y-2">
      <div class="skeleton-line h-4 w-2/3" />
      <div class="skeleton-line h-4 w-5/6" />
      <div class="skeleton-line h-4 w-1/2" />
    </div>

    <div v-else-if="expanded && result" class="space-y-3">
      <div>
        <p class="text-xs font-semibold text-slate-300">策略摘要</p>
        <p class="text-slate-100">{{ result.strategySummary }}</p>
      </div>

      <div v-if="result.topRecommendation" class="rounded-lg border border-slate-700 bg-slate-900/30 p-3">
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold text-slate-100">Top1 推荐：{{ result.topRecommendation.championName }}</p>
          <span class="text-xs font-semibold" :class="stageColorClass">{{ result.topRecommendation.levelUsed }}</span>
        </div>
        <p class="mt-2 text-xs text-slate-300">
          胜率 {{ result.topRecommendation.winRate }}% · 样本 {{ result.topRecommendation.games }} · 置信度
          {{ (result.topRecommendation.confidence * 100).toFixed(0) }}%
        </p>
        <p class="mt-2 text-xs text-slate-300">证据：{{ result.topRecommendation.evidence }}</p>
      </div>

      <div v-if="result.recommendations.length">
        <p class="text-xs font-semibold text-slate-300">候选列表</p>
        <ul class="list-disc pl-5 text-sm text-slate-200">
          <li v-for="item in result.recommendations" :key="`${item.championId}-${item.levelUsed}`">
            {{ item.championName }} ({{ item.winRate }}% / {{ item.games }} 场 / {{ item.levelUsed }})
          </li>
        </ul>
      </div>

      <div v-if="fallbackSteps.length">
        <p class="text-xs font-semibold text-slate-300">退火轨迹</p>
        <ul class="list-disc pl-5 text-sm text-slate-200">
          <li v-for="step in fallbackSteps" :key="step">{{ step }}</li>
        </ul>
      </div>
    </div>

    <p v-else class="text-sm text-slate-400">完成 FINAL PICK 后即可查看详细分析结果。</p>
  </section>
</template>

