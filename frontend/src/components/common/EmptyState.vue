<script setup lang="ts">
import CharacterIllust from '@/components/common/CharacterIllust.vue';
import type { CharacterPose } from '@/assets/characters';

interface Props {
  icon?: string;
  /** 캐릭터 그림 (지정하면 아이콘 대신 보여 준다, TSK-FE-027) */
  character?: CharacterPose;
  title: string;
  description?: string;
}

withDefaults(defineProps<Props>(), {
  icon: 'ri-inbox-line',
  character: undefined,
  description: undefined,
});
</script>

<template>
  <div class="flex flex-col items-center justify-center p-8 text-center gap-2 border-2 border-dashed border-amber-100 rounded-2xl bg-cream-50">
    <CharacterIllust v-if="character" :pose="character" size="md" class="mb-1" />
    <i v-else :class="[icon, 'text-4xl text-stone-300']"></i>
    <h4 class="font-bold text-stone-700 text-base">{{ title }}</h4>
    <p v-if="description" class="text-xs text-stone-500 max-w-sm">{{ description }}</p>
    <div v-if="$slots.action" class="mt-2">
      <slot name="action"></slot>
    </div>
  </div>
</template>
