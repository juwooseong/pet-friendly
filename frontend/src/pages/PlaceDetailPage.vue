<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import apiClient from '@/api/apiClient';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';

const route = useRoute();
const placeId = route.params.id as string;
const place = ref<any>(null);
const loading = ref(true);

onMounted(async () => {
  try {
    const res = await apiClient.get(`/places/${placeId}`);
    if (res.data && res.data.success) {
      place.value = res.data.data;
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
    <LoadingSpinner v-if="loading" text="장소 상세 정보 로딩 중..." />
    <div v-else-if="place" class="bg-white p-8 rounded-3xl border border-stone-100 shadow-sm space-y-4">
      <div class="flex justify-between items-start">
        <div>
          <span class="bg-amber-100 text-amber-700 text-xs font-bold px-2.5 py-1 rounded-lg">
            {{ place.categoryName || place.category }}
          </span>
          <h1 class="text-3xl font-extrabold text-stone-800 mt-2">{{ place.name }}</h1>
          <p class="text-stone-500 text-sm mt-1">{{ place.address }}</p>
        </div>
        <div class="text-amber-500 font-extrabold text-2xl">
          ★ {{ place.rating ? place.rating.toFixed(1) : '5.0' }}
        </div>
      </div>
    </div>
    <div v-else class="text-center py-12 text-stone-500">
      장소 정보를 찾을 수 없습니다.
    </div>
  </div>
</template>
