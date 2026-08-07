<script setup lang="ts">
</script>

<template>
  <div class="min-h-screen bg-amber-50/50 flex flex-col justify-center items-center p-4">
    <div class="mb-6 text-center">
      <router-link to="/" class="inline-flex items-center gap-2 text-amber-500 font-extrabold text-3xl">
        <i class="ri-footprint-fill"></i>
        <span>PetSpot</span>
      </router-link>
      <p class="text-xs text-stone-500 mt-1">댕냥이 동반 안심 지도 스마트 서비스</p>
    </div>

    <div class="w-full max-w-md bg-white rounded-2xl shadow-xl border border-stone-100 p-6 sm:p-8">
      <slot></slot>
    </div>

    <div class="mt-6 text-xs text-stone-400">
      <router-link to="/" class="hover:underline">← 메인 홈으로 돌아가기</router-link>
    </div>
  </div>
</template>
