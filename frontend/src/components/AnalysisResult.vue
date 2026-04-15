<script setup lang="ts">
import { ref, watch } from 'vue';
import type { AnalysisResult as AnalysisResultType } from '@/types/draft';

interface Props {
  result: AnalysisResultType | null;
  loading: boolean;
}

const props = defineProps<Props>();
const expanded = ref(false);

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
      <p class="text-slate-100">{{ result.teamCompAnalysis }}</p>
      <div>
        <p class="mb-1 text-xs text-slate-300">胜率预测</p>
        <div class="h-2 w-full overflow-hidden rounded-full bg-slate-700">
          <div class="h-full bg-cyan-400" :style="{ width: `${result.winProbability.blue}%` }" />
        </div>
        <p class="mt-1 text-xs text-slate-300">蓝方 {{ result.winProbability.blue }}% / 红方 {{ result.winProbability.red }}%</p>
      </div>
      <div>
        <p class="text-xs font-semibold text-slate-300">建议</p>
        <ul class="list-disc pl-5 text-sm text-slate-200">
          <li v-for="item in result.recommendations" :key="item">{{ item }}</li>
        </ul>
      </div>
      <div>
        <p class="text-xs font-semibold text-slate-300">克制关系</p>
        <ul class="list-disc pl-5 text-sm text-slate-200">
          <li v-for="item in result.counters" :key="item">{{ item }}</li>
        </ul>
      </div>
    </div>

    <p v-else class="text-sm text-slate-400">完成 FINAL PICK 后即可查看详细分析结果。</p>
  </section>
</template>

