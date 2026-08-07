<script setup lang="ts">
import { ref, onMounted } from 'vue';
import apiClient from '@/api/apiClient';
import BaseCard from '@/components/common/BaseCard.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';

const pets = ref<any[]>([]);
const loading = ref(true);

onMounted(async () => {
  try {
    const res = await apiClient.get('/pets');
    if (res.data && res.data.success) {
      pets.value = res.data.data || [];
    }
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <h1 class="text-2xl font-bold text-stone-800">🐾 내 반려동물 관리</h1>
      <button class="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white font-bold text-sm rounded-xl transition">
        + 반려동물 등록
      </button>
    </div>

    <LoadingSpinner v-if="loading" text="반려동물 정보 로딩 중..." />
    <div v-else-if="pets.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <BaseCard v-for="pet in pets" :key="pet.id">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 bg-amber-100 text-amber-600 rounded-full flex items-center justify-center font-bold text-lg">
            🐶
          </div>
          <div>
            <h3 class="font-bold text-stone-800">{{ pet.name }} ({{ pet.breed }})</h3>
            <p class="text-xs text-stone-500">{{ pet.weightKg }}kg · {{ pet.sizeCategory }}</p>
          </div>
        </div>
      </BaseCard>
    </div>
    <div v-else class="text-center py-12 text-stone-500 bg-white rounded-2xl border border-stone-100">
      등록된 반려동물이 없습니다.
    </div>
  </div>
</template>
