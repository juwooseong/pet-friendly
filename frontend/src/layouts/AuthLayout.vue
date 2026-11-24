<script setup lang="ts">
import BrandLogo from '@/components/common/BrandLogo.vue';
</script>

<template>
  <div class="min-h-screen bg-linear-to-b from-cream-100 via-cream-50 to-mint-50 flex flex-col justify-center items-center p-4">
    <div class="mb-6 text-center">
      <router-link to="/" class="inline-flex">
        <BrandLogo size="lg" />
      </router-link>
      <p class="text-xs text-stone-500 mt-1">헛걸음 없는 반려동물 여행</p>
    </div>

    <div class="w-full max-w-md bg-white rounded-3xl shadow-[0_12px_32px_rgba(120,72,20,0.10)] border border-stone-100 p-6 sm:p-8">
      <slot></slot>
    </div>

    <div class="mt-6 text-xs text-stone-400">
      <router-link to="/" class="inline-block py-2 hover:underline">← 메인 홈으로 돌아가기</router-link>
    </div>
  </div>
</template>
