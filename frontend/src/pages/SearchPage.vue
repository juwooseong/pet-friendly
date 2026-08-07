<script setup lang="ts">
import { ref, onMounted } from 'vue';
import apiClient from '@/api/apiClient';
import BaseCard from '@/components/common/BaseCard.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';

const keyword = ref('');
const category = ref('');
const loading = ref(false);
const places = ref<any[]>([]);

const searchPlaces = async () => {
  loading.value = true;
  try {
    const params: any = { page: 0, size: 20 };
    if (keyword.value) params.keyword = keyword.value;
    if (category.value) params.category = category.value;

    const response = await apiClient.get('/places/search', { params });
    if (response.data && response.data.success) {
      places.value = response.data.data.content || [];
    }
  } catch (err) {
    console.error('Search error:', err);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  searchPlaces();
});
</script>

<template>
  <div class="space-y-6">
    <div class="bg-white p-6 rounded-2xl border border-stone-100 shadow-sm space-y-4">
      <h2 class="text-xl font-bold text-stone-800">🔍 동반 장소 검색</h2>
      <div class="flex flex-col sm:flex-row gap-3">
        <input
          v-model="keyword"
          type="text"
          placeholder="장소명 또는 주소를 입력하세요"
          class="flex-1 px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
          @keyup.enter="searchPlaces"
        />
        <select
          v-model="category"
          class="px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
        >
          <option value="">전체 카테고리</option>
          <option value="CAFE">카페</option>
          <option value="HOTEL">숙소/펜션</option>
          <option value="PARK">공원/놀이터</option>
          <option value="HOSPITAL">동물병원</option>
          <option value="SALON">미용/스파</option>
        </select>
        <button
          @click="searchPlaces"
          class="px-6 py-2.5 bg-amber-500 hover:bg-amber-600 text-white font-bold text-sm rounded-xl transition"
        >
          검색
        </button>
      </div>
    </div>

    <LoadingSpinner v-if="loading" text="장소 목록을 검색 중입니다..." />

    <div v-else-if="places.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <BaseCard v-for="place in places" :key="place.id" hoverable @click="$router.push(`/place/${place.id}`)">
        <h3 class="font-bold text-stone-800 text-base truncate">{{ place.name }}</h3>
        <p class="text-xs text-stone-500 truncate mt-1">{{ place.address }}</p>
        <div class="mt-3 flex items-center justify-between text-xs text-amber-600">
          <span>★ {{ place.rating ? place.rating.toFixed(1) : '5.0' }}</span>
          <span>상세보기 →</span>
        </div>
      </BaseCard>
    </div>

    <div v-else class="text-center py-12 bg-white rounded-2xl border border-stone-100 p-8 text-stone-500 text-sm">
      검색 결과가 없습니다.
    </div>
  </div>
</template>
