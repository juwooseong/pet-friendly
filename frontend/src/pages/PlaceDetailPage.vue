<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import { useAuthStore } from '@/stores/authStore';
import type { ApiResponse } from '@/types/api';
import type { PlaceSearchItem } from '@/types/place';
import type { FavoriteItem } from '@/types/favorite';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const placeId = route.params.id as string;

const place = ref<PlaceSearchItem | null>(null);
const loading = ref(true);
const notFound = ref(false);
const errorMessage = ref('');

const isFavorited = ref(false);
const favoriteActionLoading = ref(false);
const favoriteErrorMessage = ref('');

const sizeLabel = (size: string): string => {
  if (size === 'SMALL') return '소형견';
  if (size === 'MEDIUM') return '중형견';
  if (size === 'LARGE') return '대형견';
  return size;
};

const fetchPlaceDetail = async () => {
  loading.value = true;
  notFound.value = false;
  errorMessage.value = '';
  try {
    const response = await apiClient.get<ApiResponse<PlaceSearchItem>>(`/places/${placeId}`);
    if (response.data && response.data.success) {
      place.value = response.data.data;
    }
  } catch (err: any) {
    if (err?.response?.status === 404) {
      notFound.value = true;
    } else {
      errorMessage.value = extractErrorMessage(err, '장소 정보를 불러오는 중 오류가 발생했습니다.');
    }
  } finally {
    loading.value = false;
  }
};

/** 기존 즐겨찾기 목록 조회 API(GET /users/me/favorites)를 재사용해 현재 장소의 즐겨찾기 여부를 확인한다. */
const fetchFavoriteStatus = async () => {
  if (!authStore.isAuthenticated) return;
  try {
    const response = await apiClient.get<ApiResponse<FavoriteItem[]>>('/users/me/favorites');
    if (response.data && response.data.success) {
      const favorites = response.data.data || [];
      isFavorited.value = favorites.some((fav) => fav.placeId === placeId);
    }
  } catch (err) {
    console.error('Failed to fetch favorite status:', err);
  }
};

const toggleFavorite = async () => {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } });
    return;
  }
  if (favoriteActionLoading.value) return;

  favoriteActionLoading.value = true;
  favoriteErrorMessage.value = '';
  const wasFavorited = isFavorited.value;
  try {
    if (wasFavorited) {
      await apiClient.delete(`/favorites/${placeId}`);
      isFavorited.value = false;
    } else {
      await apiClient.post(`/favorites/${placeId}`);
      isFavorited.value = true;
    }
  } catch (err) {
    // 요청 실패 시 화면 상태는 요청 이전 값을 그대로 유지한다 (낙관적 갱신을 하지 않으므로 별도 롤백 불필요).
    favoriteErrorMessage.value = extractErrorMessage(
      err,
      wasFavorited ? '즐겨찾기 해제 중 오류가 발생했습니다.' : '즐겨찾기 추가 중 오류가 발생했습니다.'
    );
  } finally {
    favoriteActionLoading.value = false;
  }
};

onMounted(async () => {
  await fetchPlaceDetail();
  await fetchFavoriteStatus();
});
</script>

<template>
  <div class="space-y-6">
    <LoadingSpinner v-if="loading" text="장소 상세 정보 로딩 중..." />

    <EmptyState
      v-else-if="notFound"
      icon="ri-map-pin-line"
      title="장소 정보를 찾을 수 없습니다."
      description="삭제되었거나 잘못된 주소일 수 있습니다."
    />

    <ErrorMessage v-else-if="errorMessage" :message="errorMessage" />

    <div v-else-if="place" class="bg-white p-8 rounded-3xl border border-stone-100 shadow-sm space-y-4">
      <div class="h-56 bg-stone-100 rounded-2xl overflow-hidden">
        <img
          v-if="place.imageUrl"
          :src="place.imageUrl"
          :alt="place.name"
          class="w-full h-full object-cover"
        />
        <div v-else class="w-full h-full flex items-center justify-center text-stone-300">
          <i class="ri-image-line text-4xl"></i>
        </div>
      </div>

      <div class="flex justify-between items-start">
        <div>
          <span class="bg-amber-100 text-amber-700 text-xs font-bold px-2.5 py-1 rounded-lg">
            {{ place.category }}
          </span>
          <h1 class="text-3xl font-extrabold text-stone-800 mt-2">{{ place.name }}</h1>
          <p class="text-stone-500 text-sm mt-1">{{ place.address }}</p>
        </div>
        <div class="flex flex-col items-end gap-2">
          <button
            type="button"
            @click="toggleFavorite"
            :disabled="favoriteActionLoading"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-sm font-bold border transition disabled:opacity-50 disabled:cursor-not-allowed"
            :class="
              isFavorited
                ? 'bg-rose-50 border-rose-200 text-rose-500 hover:bg-rose-100'
                : 'bg-stone-50 border-stone-200 text-stone-500 hover:bg-stone-100'
            "
          >
            <i :class="isFavorited ? 'ri-heart-fill' : 'ri-heart-line'"></i>
            <span>{{ isFavorited ? '즐겨찾기됨' : '즐겨찾기' }}</span>
          </button>
          <div class="text-amber-500 font-extrabold text-2xl text-right">
            <div>★ {{ place.rating ? place.rating.toFixed(1) : '5.0' }}</div>
            <div class="text-stone-400 text-xs font-normal mt-0.5">리뷰 {{ place.reviewCount }}개</div>
          </div>
        </div>
      </div>

      <ErrorMessage v-if="favoriteErrorMessage" :message="favoriteErrorMessage" />

      <div v-if="place.allowedSizes && place.allowedSizes.length > 0" class="flex flex-wrap gap-1.5">
        <span
          v-for="size in place.allowedSizes"
          :key="size"
          class="text-xs font-bold px-2.5 py-1 rounded-full bg-amber-50 text-amber-600 border border-amber-100"
        >
          {{ sizeLabel(size) }}
        </span>
      </div>

      <div class="pt-4 border-t border-stone-100 grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm text-stone-600">
        <div v-if="place.phone" class="flex items-center gap-2">
          <i class="ri-phone-line text-amber-500"></i>
          <span>{{ place.phone }}</span>
        </div>
        <div class="flex items-center gap-2">
          <i class="ri-map-pin-line text-amber-500"></i>
          <span>{{ place.latitude }}, {{ place.longitude }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
