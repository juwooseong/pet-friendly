<script setup lang="ts">
import BrandLogo from '@/components/common/BrandLogo.vue';
import CharacterIllust from '@/components/common/CharacterIllust.vue';
import { useRoute } from 'vue-router';

const route = useRoute();
</script>

<template>
  <div class="min-h-screen bg-linear-to-b from-cream-100 via-cream-50 to-mint-50 flex flex-col justify-center items-center p-4">
    <div class="mb-6 text-center">
      <router-link to="/" class="inline-flex">
        <BrandLogo size="lg" />
      </router-link>
      <p class="text-xs text-stone-500 mt-1">헛걸음 없는 반려동물 여행</p>
    </div>

    <div class="relative w-full max-w-md mt-10">
      <!-- 카드 위로 고개를 내민 콩이와 나비 (TSK-FE-029) - 장식 -->
      <!-- 404는 지도 든 콩이가 따로 나오므로 얼굴은 뺀다 -->
      <div
        v-if="route.name !== 'NotFound'"
        class="absolute -top-10 inset-x-0 flex justify-center gap-6 pointer-events-none"
        data-testid="auth-characters"
      >
        <CharacterIllust pose="kongi-face" size="xs" loading="eager" />
        <CharacterIllust pose="nabi-face" size="xs" loading="eager" />
      </div>
      <div class="relative bg-white rounded-3xl shadow-[0_12px_32px_rgba(120,72,20,0.10)] border border-stone-100 p-6 sm:p-8">
        <slot></slot>
      </div>
    </div>

    <div class="mt-6 text-xs text-stone-400">
      <router-link to="/" class="inline-block py-2 hover:underline">← 메인 홈으로 돌아가기</router-link>
    </div>
  </div>
</template>
