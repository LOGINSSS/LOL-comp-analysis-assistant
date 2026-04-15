<script setup lang="ts">
import type { Champion } from '@/types/draft';

interface Props {
  phaseText: string;
  bans: Array<number | null>;
  championMap: Map<number, Champion>;
  activeBanIndex: number;
}

defineProps<Props>();
</script>

<template>
  <header class="panel mb-4">
    <div class="mb-3 text-center">
      <h1 class="phase-title">{{ phaseText }}</h1>
    </div>
    <div class="ban-board">
      <div class="ban-group">
        <button
          v-for="(ban, index) in bans.slice(0, 5)"
          :key="`blue-ban-${index}`"
          type="button"
          class="ban-slot"
          :class="activeBanIndex === index ? 'slot-active' : ''"
          :disabled="activeBanIndex !== index"
        >
          <img v-if="ban && championMap.get(ban)" class="avatar-img" :src="championMap.get(ban)!.imageUrl" :alt="championMap.get(ban)!.name" />
          <span v-else>+</span>
          <i v-if="ban" class="banned-mark">X</i>
        </button>
      </div>

      <div class="ban-divider">BAN</div>

      <div class="ban-group">
        <button
          v-for="(ban, localIndex) in bans.slice(5, 10)"
          :key="`red-ban-${localIndex}`"
          type="button"
          class="ban-slot"
          :class="activeBanIndex === localIndex + 5 ? 'slot-active' : ''"
          :disabled="activeBanIndex !== localIndex + 5"
        >
          <img v-if="ban && championMap.get(ban)" class="avatar-img" :src="championMap.get(ban)!.imageUrl" :alt="championMap.get(ban)!.name" />
          <span v-else>+</span>
          <i v-if="ban" class="banned-mark">X</i>
        </button>
      </div>
    </div>
  </header>
</template>

