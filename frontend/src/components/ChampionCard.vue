<script setup lang="ts">
import type { Champion } from '@/types/draft';

interface Props {
  champion: Champion;
  state: 'available' | 'banned' | 'picked-blue' | 'picked-red';
  selected?: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  pick: [championId: number];
}>();

function onClick() {
  emit('pick', props.champion.id);
}
</script>

<template>
  <button
    type="button"
    class="champion-card group"
    :class="[
      selected ? 'ring-2 ring-cyan-400' : '',
      state === 'banned' ? 'is-banned' : '',
      state === 'picked-blue' ? 'is-picked-blue' : '',
      state === 'picked-red' ? 'is-picked-red' : ''
    ]"
    @click="onClick"
  >
    <div
      v-motion
      :initial="{ opacity: 0, scale: 0.95 }"
      :enter="{ opacity: 1, scale: 1 }"
      class="relative avatar-frame avatar-card mx-auto"
    >
      <img :src="champion.imageUrl" :alt="champion.name" class="avatar-img" />
      <div v-if="state === 'banned'" class="banned-mark">X</div>
    </div>
    <p class="champion-card-name">{{ champion.name }}</p>
  </button>
</template>

