<script setup lang="ts">
import CharacterIllust from '@/components/common/CharacterIllust.vue';

interface Props {
  message: string;
  /** 화면을 불러오지 못한 경우처럼 화면 전체 오류면 길 잃은 콩이를 함께 보여 준다 (입력 오류에는 쓰지 않는다, TSK-FE-027) */
  illustrated?: boolean;
}

defineProps<Props>();
</script>

<template>
  <div
    v-if="illustrated"
    class="flex flex-col items-center gap-2 p-6 bg-rose-50/60 border border-rose-200 text-rose-700 text-sm rounded-2xl text-center"
    role="alert"
  >
    <CharacterIllust pose="kongi-lost" size="md" />
    <span>{{ message }}</span>
  </div>
  <div v-else class="flex items-center gap-2 p-4 bg-rose-50 border border-rose-200 text-rose-700 text-sm rounded-xl" role="alert">
    <i class="ri-error-warning-line text-lg flex-shrink-0"></i>
    <span>{{ message }}</span>
  </div>
</template>
