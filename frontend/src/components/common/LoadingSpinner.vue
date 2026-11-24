<script setup lang="ts">
import CharacterIllust from '@/components/common/CharacterIllust.vue';

/** 화면·영역 로딩 (TSK-FE-027): 냄새 맡는 콩이. 버튼 안 로딩은 각 버튼의 작은 회전 표시를 그대로 쓴다. */
interface Props {
  text?: string;
}

defineProps<Props>();
</script>

<template>
  <div class="flex flex-col items-center justify-center p-8 gap-2" role="status" aria-live="polite">
    <CharacterIllust pose="kongi-sniff" size="sm" loading="eager" class="motion-safe:animate-sniff" />
    <span class="text-xs font-semibold text-stone-500">{{ text || '킁킁, 찾는 중…' }}</span>
  </div>
</template>
