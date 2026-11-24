<script setup lang="ts">
import BaseButton from '@/components/common/BaseButton.vue';
import CharacterSpeech from '@/components/common/CharacterSpeech.vue';
</script>

<template>
  <!-- 지도를 거꾸로 든 콩이 (TSK-FE-029, docs/BRAND.md 7장) -->
  <div class="min-h-[60vh] flex flex-col items-center justify-center p-4">
    <div class="w-full max-w-sm space-y-5 text-center">
      <CharacterSpeech pose="kongi-lost" speaker="콩이" size="md" class="text-left">
        어라, 지도를 거꾸로 봤나 봐! 여기는 없는 길이야.
      </CharacterSpeech>
      <div class="space-y-1">
        <h1 class="text-lg font-bold text-stone-800">페이지를 찾을 수 없습니다 (404)</h1>
        <p class="text-sm text-stone-500">요청하신 페이지가 존재하지 않거나 이동되었을 수 있습니다.</p>
      </div>
      <BaseButton @click="$router.push('/')" variant="primary">홈으로 돌아가기</BaseButton>
    </div>
  </div>
</template>
