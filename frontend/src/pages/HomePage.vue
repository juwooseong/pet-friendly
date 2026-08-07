<script setup lang="ts">
import { ref, onMounted } from 'vue';
import apiClient from '@/api/apiClient';
import BaseCard from '@/components/common/BaseCard.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';

interface PlaceItem {
  id: string;
  name: string;
  category: string;
  categoryName: string;
  address: string;
  imageUrl?: string;
  rating?: number;
  reviewCount?: number;
}

const loading = ref(true);
const errorMessage = ref('');
const places = ref<PlaceItem[]>([]);

const fetchBackendData = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await apiClient.get('/places/search', {
      params: { page: 0, size: 6 }
    });
    if (response.data && response.data.success) {
      places.value = response.data.data.content || [];
    }
  } catch (err: any) {
    console.error('Backend connection test error:', err);
    errorMessage.value = '백엔드 서버 연동 중 오류가 발생했습니다: ' + (err.message || '연동 실패');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchBackendData();
});
</script>

<template>
  <div class="space-y-8">
    <!-- Hero Banner -->
    <div class="bg-gradient-to-r from-amber-500 to-amber-600 rounded-3xl p-8 text-white shadow-lg flex flex-col md:flex-row items-center justify-between gap-6">
      <div class="space-y-3 max-w-xl">
        <span class="inline-block bg-white/20 text-white text-xs font-semibold px-3 py-1 rounded-full">
          🐾 댕냥이 전용 안심 지도
        </span>
        <h1 class="text-3xl sm:text-4xl font-extrabold leading-tight">
          내 반려견 체중에 딱 맞는<br />동반 장소를 탐색해보세요!
        </h1>
        <p class="text-amber-100 text-sm">
          공공데이터 기반 실시간 장소 정보와 펫 수칙 맞춤 평가(PASS/WARN/DENY)를 제공합니다.
        </p>
        <div class="pt-2">
          <router-link
            to="/search"
            class="inline-flex items-center gap-2 bg-white text-amber-600 font-bold px-5 py-2.5 rounded-xl shadow hover:bg-amber-50 transition"
          >
            <i class="ri-search-line"></i>
            <span>지금 장소 찾아보기</span>
          </router-link>
        </div>
      </div>
      <div class="text-6xl text-white/90 hidden md:block">
        🐶🐱
      </div>
    </div>

    <!-- Backend Connection Test & Place Grid -->
    <div class="space-y-4">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-bold text-stone-800 flex items-center gap-2">
          <i class="ri-sparkles-line text-amber-500"></i>
          <span>추천 동반 장소 (Spring Boot API 연동)</span>
        </h2>
        <button @click="fetchBackendData" class="text-xs font-medium text-amber-600 hover:underline">
          새로고침
        </button>
      </div>

      <LoadingSpinner v-if="loading" text="백엔드 API에서 장소 데이터를 불러오는 중입니다..." />

      <ErrorMessage v-else-if="errorMessage" :message="errorMessage" />

      <div v-else-if="places.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <BaseCard v-for="place in places" :key="place.id" hoverable @click="$router.push(`/place/${place.id}`)">
          <div class="h-40 bg-stone-100 rounded-xl overflow-hidden mb-3 relative">
            <img
              v-if="place.imageUrl"
              :src="place.imageUrl"
              :alt="place.name"
              class="w-full h-full object-cover"
            />
            <div v-else class="w-full h-full flex items-center justify-center text-stone-300">
              <i class="ri-image-line text-3xl"></i>
            </div>
            <span class="absolute top-2 left-2 bg-white/90 backdrop-blur-sm text-stone-700 text-xs font-bold px-2.5 py-1 rounded-lg">
              {{ place.categoryName || place.category }}
            </span>
          </div>

          <h3 class="font-bold text-stone-800 text-base truncate">{{ place.name }}</h3>
          <p class="text-xs text-stone-500 truncate mt-1">{{ place.address }}</p>

          <div class="mt-3 pt-3 border-t border-stone-100 flex items-center justify-between text-xs">
            <div class="flex items-center gap-1 text-amber-500 font-bold">
              <i class="ri-star-fill"></i>
              <span>{{ place.rating ? place.rating.toFixed(1) : '5.0' }}</span>
              <span class="text-stone-400 font-normal">({{ place.reviewCount || 0 }})</span>
            </div>
            <span class="text-amber-600 font-medium hover:underline">상세보기 →</span>
          </div>
        </BaseCard>
      </div>

      <div v-else class="text-center py-12 bg-white rounded-2xl border border-stone-100 p-8">
        <p class="text-stone-500 text-sm">등록된 장소가 없습니다.</p>
      </div>
    </div>
  </div>
</template>
