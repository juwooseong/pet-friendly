<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { formatRating } from '@/utils/rating';
import { TOUR_API_SOURCE } from '@/utils/publicData';
import PlaceImage from '@/components/common/PlaceImage.vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { usePetStore } from '@/stores/petStore';
import { usePlaceStore } from '@/stores/placeStore';
import type { PlaceSearchItem } from '@/types/place';
import BaseCard from '@/components/common/BaseCard.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const router = useRouter();
const authStore = useAuthStore();
const petStore = usePetStore();
/** 검색 조건·결과는 스토어에 두어 상세에서 돌아와도 유지된다 (TSK-FE-015 15-3) */
const placeStore = usePlaceStore();
const {
  keyword,
  category,
  sizeCategory,
  sizeVerifiedOnly,
  radiusKm,
  distanceSortEnabled,
  userCoords,
  sizeDefaultPet: representativePet,
  places,
  currentPage,
  totalPages,
  totalElements,
  hasNext,
  loading,
  errorMessage,
} = storeToRefs(placeStore);

/**
 * 크기 필터 결과에서 이 장소의 크기 조건이 확인되지 않았는지(allowedSizes 없음).
 * 서버가 허용 안 됨이 확인된 장소는 이미 제외하므로, 목록에 남은 장소는 "허용 확인" 또는 "정보 없음"뿐이다.
 */
const isSizeUnverified = (place: PlaceSearchItem) =>
  !!sizeCategory.value && (!place.allowedSizes || place.allowedSizes.length === 0);
const geolocationError = ref('');

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
    // 반려동물 화면 등에서 이미 받은 목록이 있으면 다시 조회하지 않는다 (TSK-FE-015)
    await petStore.ensurePets();
    const representative = petStore.representativePet;
    if (representative) {
      representativePet.value = representative;
      sizeCategory.value = representative.sizeCategory;
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

const searchPlaces = (page = 0) => placeStore.search(page);

const goToPage = async (page: number) => {
  if (page < 0 || page >= totalPages.value || page === currentPage.value) return;
  await searchPlaces(page);
  // 페이지 버튼은 목록 맨 아래에 있어서, 스크롤을 그대로 두면 새 목록이 보이지 않고 숫자만 바뀐 것처럼 보인다.
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

/**
 * 브라우저 뒤로/앞으로 가기로 이 화면에 돌아왔는지.
 * Vue Router는 기록 항목마다 다음 항목(forward)을 저장하는데, 메뉴·링크로 새로 들어오면 비어 있다.
 */
const isHistoryReturn = () => !!window.history.state?.forward;

onMounted(async () => {
  // 상세 등에서 뒤로 돌아왔으면 이전 검색 상태를 그대로 보여 준다
  if (placeStore.searched && isHistoryReturn()) return;

  placeStore.$reset();
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
          <option value="SHOPPING">쇼핑</option>
          <option value="CULTURE">문화시설</option>
          <option value="FESTIVAL">축제·공연·행사</option>
          <option value="COURSE">여행코스</option>
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

      <div v-if="sizeCategory" class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-stone-500" data-testid="size-filter-info">
        <label class="inline-flex items-center gap-1.5 cursor-pointer select-none">
          <input
            v-model="sizeVerifiedOnly"
            type="checkbox"
            class="accent-amber-500"
            data-testid="size-verified-only"
            @change="searchPlaces(0)"
          />
          <span class="font-bold text-stone-600">{{ sizeLabel(sizeCategory) }} 동반이 확인된 곳만 보기</span>
        </label>
        <span v-if="!sizeVerifiedOnly">
          확인된 곳을 먼저 보여 주고, 크기 정보가 없는 곳은 '크기 조건 확인 필요'로 표시합니다.
          {{ sizeLabel(sizeCategory) }} 동반이 안 된다고 확인된 곳은 제외됩니다.
        </span>
      </div>

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

      <!-- 휴대폰: 작은 사진을 왼쪽에 둔 목록형(스크롤 길이 절반 이하), 넓은 화면: 큰 사진 카드 (TSK-FE-024) -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-6">
        <BaseCard v-for="place in places" :key="place.id" hoverable @click="router.push(`/place/${place.id}`)">
          <div class="flex gap-3 sm:block">
          <div
            class="w-24 h-24 shrink-0 rounded-xl sm:w-auto sm:h-36 sm:-mx-5 sm:-mt-5 sm:mb-3 sm:rounded-none sm:rounded-t-2xl bg-stone-100 overflow-hidden"
            data-testid="place-thumbnail"
          >
            <PlaceImage
              :thumbnail-url="place.thumbnailUrl"
              :image-url="place.imageUrl"
              :alt="place.name"
              :category="place.category"
              :copyright-type="place.imageCopyrightType"
              icon-class="text-2xl sm:text-3xl"
            />
          </div>
          <div class="min-w-0 flex-1">
          <span v-if="place.categoryName" class="text-[11px] font-bold text-amber-700">{{ place.categoryName }}</span>
          <h3 class="font-bold text-stone-800 text-base truncate">{{ place.name }}</h3>
          <p class="text-xs text-stone-500 truncate mt-1">{{ place.address }}</p>
          <div class="mt-2 flex flex-wrap gap-1">
            <span
              v-for="size in place.allowedSizes || []"
              :key="size"
              class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-mint-50 text-mint-700 border border-mint-200"
            >
              {{ sizeLabel(size) }}
            </span>
            <span
              v-if="isSizeUnverified(place)"
              class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-stone-100 text-stone-500 border border-stone-200"
              data-testid="size-unverified-badge"
              title="공공데이터에 반려동물 크기 정보가 없어요. 방문 전 장소에 확인해 주세요."
            >
              크기 조건 확인 필요
            </span>
          </div>
          <div class="mt-2 sm:mt-3 flex items-center justify-between gap-2 text-xs text-amber-600">
            <span>★ {{ formatRating(place.rating, place.reviewCount) }}</span>
            <span v-if="place.distanceKm !== null" class="text-stone-500">{{ place.distanceKm }}km</span>
            <!-- 휴대폰에서는 카드 전체를 누르므로 안내 문구 생략 -->
            <span class="hidden sm:inline">상세보기 →</span>
          </div>
          </div>
          </div>
        </BaseCard>
      </div>

      <!-- 휴대폰은 터치 영역 40px, 현재 페이지 앞뒤 1개씩만 표시해 한 줄에 들어가게 한다 (TSK-FE-024) -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-1.5 pt-2">
        <button
          :disabled="currentPage === 0"
          @click="goToPage(currentPage - 1)"
          class="px-3 h-10 sm:h-8 text-sm rounded-lg border border-stone-200 text-stone-600 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-stone-50"
        >
          이전
        </button>
        <button
          v-for="page in pageNumbers"
          :key="page"
          @click="goToPage(page)"
          class="w-10 h-10 sm:w-8 sm:h-8 text-sm rounded-lg transition items-center justify-center"
          :class="[
            page === currentPage
              ? 'bg-amber-500 text-white font-bold'
              : 'text-stone-600 border border-stone-200 hover:bg-stone-50',
            Math.abs(page - currentPage) > 1 ? 'hidden sm:inline-flex' : 'inline-flex',
          ]"
        >
          {{ page + 1 }}
        </button>
        <button
          :disabled="!hasNext"
          @click="goToPage(currentPage + 1)"
          class="px-3 h-10 sm:h-8 text-sm rounded-lg border border-stone-200 text-stone-600 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-stone-50"
        >
          다음
        </button>
      </div>

      <p class="text-[11px] text-stone-400 text-center" data-testid="data-source">
        장소 정보와 사진 일부는 {{ TOUR_API_SOURCE }}에서 제공받았습니다(공공누리).
      </p>
    </template>

    <EmptyState v-else icon="ri-search-line" title="검색 결과가 없습니다." description="키워드나 필터 조건을 변경해 다시 검색해보세요." />
  </div>
</template>
