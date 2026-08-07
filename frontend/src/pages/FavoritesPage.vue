<script setup lang="ts">
import { ref, onMounted } from 'vue';
import apiClient from '@/api/apiClient';
import BaseCard from '@/components/common/BaseCard.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';

const favorites = ref<any[]>([]);
const loading = ref(true);

onMounted(async () => {
  try {
    const res = await apiClient.get('/users/me/favorites');
    if (res.data && res.data.success) {
      favorites.value = res.data.data || [];
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
    <h1 class="text-2xl font-bold text-stone-800">⭐ 내 즐겨찾기 장소</h1>
    <LoadingSpinner v-if="loading" text="즐겨찾기 목록 불러오는 중..." />
    <div v-else-if="favorites.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <BaseCard v-for="fav in favorites" :key="fav.id">
        <h3 class="font-bold text-stone-800">{{ fav.placeName }}</h3>
        <p class="text-xs text-stone-500 mt-1">{{ fav.address }}</p>
      </BaseCard>
    </div>
    <div v-else class="text-center py-12 text-stone-500 bg-white rounded-2xl border border-stone-100">
      등록된 즐겨찾기가 없습니다.
    </div>
  </div>
</template>
