<script setup lang="ts">
import CharacterIllust from './CharacterIllust.vue';
import type { CharacterPose } from '@/assets/characters';

/**
 * 캐릭터 말풍선 (TSK-FE-029, docs/BRAND.md 6장). 한 화면에 하나만 쓴다.
 * 말투는 캐릭터를 따른다 - 콩이 반말, 나비 존댓말. 그림은 장식이고 말하는 이 이름과 내용은 글로 전한다.
 */
withDefaults(
  defineProps<{
    pose: CharacterPose;
    /** 말하는 캐릭터 이름 (말풍선 위에 작게 표시) */
    speaker: '콩이' | '나비';
    size?: 'xs' | 'sm' | 'md';
  }>(),
  { size: 'xs' }
);
</script>

<template>
  <div class="flex items-end gap-2" data-testid="character-speech">
    <CharacterIllust :pose="pose" :size="size" class="shrink-0" />
    <div class="relative min-w-0 flex-1 rounded-2xl rounded-bl-sm border border-mint-200 bg-mint-50 px-3.5 py-2.5 text-sm text-stone-700">
      <p class="text-[11px] font-bold text-mint-700">{{ speaker }}</p>
      <p class="mt-0.5 leading-relaxed"><slot /></p>
    </div>
  </div>
</template>
