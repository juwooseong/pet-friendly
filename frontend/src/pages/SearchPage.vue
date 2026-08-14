<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '@/api/apiClient';
import { useAuthStore } from '@/stores/authStore';
import type { ApiResponse, PageResponse } from '@/types/api';
import type { PlaceCategory, PlaceSearchItem, PlaceSearchParams } from '@/types/place';
import type { Pet, SizeCategory } from '@/types/user';
import BaseCard from '@/components/common/BaseCard.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const router = useRouter();
const authStore = useAuthStore();

const PAGE_SIZE = 12;
const DEFAULT_RADIUS_KM = 3;

const keyword = ref('');
const category = ref<PlaceCategory | ''>('');
const sizeCategory = ref<SizeCategory | ''>('');
const radiusKm = ref(DEFAULT_RADIUS_KM);
const distanceSortEnabled = ref(false);
const userCoords = ref<{ latitude: number; longitude: number } | null>(null);
const geolocationError = ref('');

const loading = ref(false);
const errorMessage = ref('');
const places = ref<PlaceSearchItem[]>([]);
const currentPage = ref(0);
const totalPages = ref(0);
const totalElements = ref(0);
const hasNext = ref(false);

const representativePet = ref<Pet | null>(null);

const pageNumbers = computed(() => {
  if (totalPages.value <= 1) return [];
  const start = Math.max(0, currentPage.value - 2);
  const end = Math.min(totalPages.value - 1, start + 4);
  const pages: number[] = [];
  for (let i = start; i <= end; i++) pages.push(i);
  return pages;
});

const sizeLabel = (size: string): string => {
  if (size === 'SMALL') return '소형견';
  if (size === 'MEDIUM') return '중형견';
  if (size === 'LARGE') return '대형견';
  return size;
};

/** 로그인 사용자의 대표 반려동물을 조회해 크기 필터 기본값으로 사용한다. */
const fetchRepresentativePet = async () => {
  if (!authStore.isAuthenticated) return;
  try {
    const response = await apiClient.get<ApiResponse<Pet[]>>('/pets');
    if (response.data?.success) {
      const pets = response.data.data || [];
      const representative = pets.find((p) => p.representative) || pets[0] || null;
      if (representative) {
        representativePet.value = representative;
        sizeCategory.value = representative.sizeCategory;
      }
    }
  } catch (err) {
    console.error('Failed to fetch representative pet:', err);
  }
};

const requestGeolocation = (): Promise<{ latitude: number; longitude: number } | null> => {
  return new Promise((resolve) => {
    if (!('geolocation' in navigator)) {
      geolocationError.value = '이 브라우저는 위치 정보를 지원하지 않습니다.';
      resolve(null);
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        geolocationError.value = '';
        resolve({ latitude: position.coords.latitude, longitude: position.coords.longitude });
      },
      () => {
        geolocationError.value = '위치 정보 접근이 거부되어 거리순 정렬을 사용할 수 없습니다.';
        resolve(null);
      },
      { timeout: 5000 }
    );
  });
};

const toggleDistanceSort = async () => {
  if (distanceSortEnabled.value) {
    // 이미 켜져있으면 끄기
    distanceSortEnabled.value = false;
    userCoords.value = null;
    await searchPlaces(0);
    return;
  }

  const coords = await requestGeolocation();
  if (!coords) {
    distanceSortEnabled.value = false;
    return;
  }
  userCoords.value = coords;
  distanceSortEnabled.value = true;
  await searchPlaces(0);
};

const searchPlaces = async (page = 0) => {
  loading.value = true;
  errorMessage.value = '';
  try {
    const params: PlaceSearchParams = { page, size: PAGE_SIZE };
    if (keyword.value) params.keyword = keyword.value;
    if (category.value) params.category = category.value;
    if (sizeCategory.value) params.sizeCategory = sizeCategory.value;
    if (distanceSortEnabled.value && userCoords.value) {
      params.latitude = userCoords.value.latitude;
      params.longitude = userCoords.value.longitude;
      params.radiusKm = radiusKm.value;
    }

    const response = await apiClient.get<ApiResponse<PageResponse<PlaceSearchItem>>>('/places/search', { params });
    if (response.data && response.data.success) {
      const pageData = response.data.data;
      places.value = pageData.content || [];
      currentPage.value = pageData.page;
      totalPages.value = pageData.totalPages;
      totalElements.value = pageData.totalElements;
      hasNext.value = pageData.hasNext;
    }
  } catch (err) {
    console.error('Search error:', err);
    errorMessage.value = '장소 검색 중 오류가 발생했습니다.';
  } finally {
    loading.value = false;
  }
};

const goToPage = (page: number) => {
  if (page < 0 || page >= totalPages.value || page === currentPage.value) return;
  searchPlaces(page);
};

onMounted(async () => {
  await fetchRepresentativePet();
  await searchPlaces(0);
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
          @keyup.enter="searchPlaces(0)"
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
        <select
          v-model="sizeCategory"
          class="px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
        >
          <option value="">반려동물 크기 전체</option>
          <option value="SMALL">소형견 (10kg 이하)</option>
          <option value="MEDIUM">중형견 (10~20kg)</option>
          <option value="LARGE">대형견 (20kg 초과)</option>
        </select>
        <button
          @click="searchPlaces(0)"
          class="px-6 py-2.5 bg-amber-500 hover:bg-amber-600 text-white font-bold text-sm rounded-xl transition"
        >
          검색
        </button>
      </div>

      <p v-if="representativePet" class="text-xs text-stone-500">
        🐾 대표 반려동물 <span class="font-bold text-amber-600">{{ representativePet.name }}</span
        >({{ sizeLabel(representativePet.sizeCategory) }}) 기준으로 크기 필터가 설정되었습니다. 필요하면 위에서 변경할 수 있습니다.
      </p>

      <div class="flex flex-wrap items-center gap-3 pt-2 border-t border-stone-100">
        <button
          @click="toggleDistanceSort"
          class="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-bold transition"
          :class="
            distanceSortEnabled
              ? 'bg-amber-500 text-white hover:bg-amber-600'
              : 'bg-stone-50 text-stone-600 border border-stone-200 hover:bg-stone-100'
          "
        >
          <i class="ri-map-pin-line"></i>
          <span>{{ distanceSortEnabled ? '거리순 정렬 중 (내 위치 기준)' : '내 위치 기준 거리순 정렬' }}</span>
        </button>

        <select
          v-if="distanceSortEnabled"
          v-model.number="radiusKm"
          @change="searchPlaces(0)"
          class="px-3 py-2 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
        >
          <option :value="1">반경 1km</option>
          <option :value="3">반경 3km</option>
          <option :value="5">반경 5km</option>
          <option :value="10">반경 10km</option>
        </select>

        <span v-if="geolocationError" class="text-xs text-rose-500">{{ geolocationError }}</span>
      </div>
    </div>

    <LoadingSpinner v-if="loading" text="장소 목록을 검색 중입니다..." />

    <ErrorMessage v-else-if="errorMessage" :message="errorMessage" />

    <template v-else-if="places.length > 0">
      <p class="text-xs text-stone-500">총 {{ totalElements }}개의 장소가 검색되었습니다.</p>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <BaseCard v-for="place in places" :key="place.id" hoverable @click="router.push(`/place/${place.id}`)">
          <h3 class="font-bold text-stone-800 text-base truncate">{{ place.name }}</h3>
          <p class="text-xs text-stone-500 truncate mt-1">{{ place.address }}</p>
          <div class="mt-2 flex flex-wrap gap-1">
            <span
              v-for="size in place.allowedSizes || []"
              :key="size"
              class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-50 text-amber-600 border border-amber-100"
            >
              {{ sizeLabel(size) }}
            </span>
          </div>
          <div class="mt-3 flex items-center justify-between text-xs text-amber-600">
            <span>★ {{ place.rating ? place.rating.toFixed(1) : '5.0' }}</span>
            <span v-if="place.distanceKm !== null" class="text-stone-500">{{ place.distanceKm }}km</span>
            <span>상세보기 →</span>
          </div>
        </BaseCard>
      </div>

      <div v-if="totalPages > 1" class="flex items-center justify-center gap-1.5 pt-2">
        <button
          :disabled="currentPage === 0"
          @click="goToPage(currentPage - 1)"
          class="px-3 py-1.5 text-sm rounded-lg border border-stone-200 text-stone-600 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-stone-50"
        >
          이전
        </button>
        <button
          v-for="page in pageNumbers"
          :key="page"
          @click="goToPage(page)"
          class="w-8 h-8 text-sm rounded-lg transition"
          :class="
            page === currentPage
              ? 'bg-amber-500 text-white font-bold'
              : 'text-stone-600 border border-stone-200 hover:bg-stone-50'
          "
        >
          {{ page + 1 }}
        </button>
        <button
          :disabled="!hasNext"
          @click="goToPage(currentPage + 1)"
          class="px-3 py-1.5 text-sm rounded-lg border border-stone-200 text-stone-600 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-stone-50"
        >
          다음
        </button>
      </div>
    </template>

    <EmptyState v-else icon="ri-search-line" title="검색 결과가 없습니다." description="키워드나 필터 조건을 변경해 다시 검색해보세요." />
  </div>
</template>
