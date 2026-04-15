<script setup lang="ts">
interface Props {
  statusText: string;
  lockEnabled: boolean;
  loading: boolean;
  confirmLabel: string;
  actionMode: 'ban' | 'pick' | 'analyze';
}

const props = defineProps<Props>();

const emit = defineEmits<{
  lock: [];
}>();
</script>

<template>
  <section class="panel mt-4 flex flex-wrap items-center justify-center gap-3">
    <p class="w-full text-center text-sm text-slate-300">{{ statusText }}</p>
    <button
      :class="[
        props.actionMode === 'ban' ? 'btn-ban' : props.actionMode === 'analyze' ? 'btn-analyze' : 'btn-primary',
        'btn-double-height'
      ]"
      :disabled="!lockEnabled || loading"
      @click="emit('lock')"
    >
      {{ loading ? '处理中...' : confirmLabel }}
    </button>
  </section>
</template>

