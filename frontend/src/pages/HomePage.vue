<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { formatRating } from '@/utils/rating';
import PlaceImage from '@/components/common/PlaceImage.vue';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import BaseCard from '@/components/common/BaseCard.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import CharacterIllust from '@/components/common/CharacterIllust.vue';
import { CHARACTER_IMAGES, DUO_TOGETHER_MOBILE_SRC } from '@/assets/characters';

const hero = CHARACTER_IMAGES['duo-together'];

/** 장소 유형 바로가기 - 검색 화면을 해당 유형으로 바로 연다 */
const SHORTCUTS = [
  { category: 'CAFE', label: '음식점·카페', icon: 'ri-restaurant-line' },
  { category: 'HOTEL', label: '숙소', icon: 'ri-hotel-bed-line' },
  { category: 'PARK', label: '공원', icon: 'ri-tree-line' },
  { category: 'HOSPITAL', label: '동물병원', icon: 'ri-hospital-line' },
  { category: 'CULTURE', label: '문화시설', icon: 'ri-ancient-gate-line' },
  { category: 'COURSE', label: '여행코스', icon: 'ri-route-line' },
] as const;

/** 짧은 스토리는 처음 방문자에게만 보인다. 저장소를 못 쓰는 환경이면 매번 보여 준다. */
const STORY_KEY = 'petspot.homeStoryDismissed';
const readStoryDismissed = () => {
  try {
    return localStorage.getItem(STORY_KEY) === '1';
  } catch {
    return false;
  }
};
const showStory = ref(!readStoryDismissed());
const dismissStory = () => {
  showStory.value = false;
  try {
    localStorage.setItem(STORY_KEY, '1');
  } catch {
    /* 저장 실패 시 이번 방문에서만 숨긴다 */
  }
};

interface PlaceItem {
  id: string;
  name: string;
  category: string;
  categoryName: string;
  address: string;
  imageUrl?: string | null;
  thumbnailUrl?: string | null;
  /** 대표 이미지 공공누리 유형 (Type3/Type4는 변경금지라 자르지 않고 표시) */
  imageCopyrightType?: string | null;
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
    errorMessage.value = extractErrorMessage(err, '추천 장소를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
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
    <!-- 첫 화면 (TSK-FE-028): 밝은 바탕 - 진한 배경에서는 그림 털 가장자리에 흰 테두리가 보인다 -->
    <section class="bg-linear-to-br from-cream-100 via-cream-50 to-mint-50 border border-amber-100 rounded-3xl p-6 sm:p-8 flex flex-col md:flex-row items-center justify-between gap-4 md:gap-8">
      <div class="space-y-3 max-w-xl w-full">
        <span class="inline-block bg-mint-50 text-mint-700 border border-mint-200 text-xs font-semibold px-3 py-1 rounded-full">
          🐾 콩이와 나비가 먼저 다녀왔어요
        </span>
        <h1 class="text-3xl sm:text-4xl font-extrabold leading-tight text-stone-800">
          헛걸음 없는<br />반려동물 여행
        </h1>
        <p class="text-stone-600 text-sm">
          우리 아이 크기로 함께 갈 수 있는 곳만 골라 보세요. 한국관광공사 공공데이터로 전국 동반 장소와 동반 조건을 알려 드려요.
        </p>
        <div class="pt-2">
          <router-link
            to="/search"
            class="inline-flex items-center gap-2 bg-amber-500 text-white font-bold px-5 py-2.5 rounded-xl shadow hover:bg-amber-600 transition"
          >
            <i class="ri-search-line"></i>
            <span>지금 장소 찾아보기</span>
          </router-link>
        </div>
      </div>
      <!-- 첫 화면에서 가장 큰 요소(LCP). index.html이 미리 받고, 휴대폰은 640px 파일만 쓴다 -->
      <picture class="w-full max-w-sm md:max-w-[420px] shrink-0">
        <source media="(max-width: 767px)" :srcset="DUO_TOGETHER_MOBILE_SRC" />
        <img
          :src="hero.src"
          :width="hero.width"
          :height="hero.height"
          alt=""
          aria-hidden="true"
          fetchpriority="high"
          class="w-full h-auto"
          data-testid="home-hero-image"
        />
      </picture>
    </section>

    <!-- 짧은 스토리: 처음 방문자에게만, 닫으면 다시 보이지 않는다 -->
    <section
      v-if="showStory"
      class="relative bg-white border border-amber-100 rounded-2xl p-5 pr-12 flex gap-4 items-start"
      data-testid="home-story"
    >
      <CharacterIllust pose="nabi-face" size="sm" class="shrink-0 hidden sm:block" />
      <div class="space-y-1.5 text-sm text-stone-600">
        <h2 class="font-bold text-stone-800">콩이의 첫 번째 지도</h2>
        <p>신나게 달려간 카페 앞에서 "반려동물 출입 금지" 팻말을 만난 콩이.</p>
        <p>나비가 돋보기를 들고 "확인부터 해요"라며 동반 조건을 하나씩 짚었어요.</p>
        <p>둘이 발자국을 찍어 만든 지도, 그게 PetSpot이에요.</p>
      </div>
      <button
        type="button"
        class="absolute top-2 right-2 w-10 h-10 flex items-center justify-center rounded-full text-stone-400 hover:text-stone-600 hover:bg-stone-100"
        aria-label="스토리 닫기"
        @click="dismissStory"
      >
        <i class="ri-close-line text-lg"></i>
      </button>
    </section>

    <!-- 장소 유형 바로가기 -->
    <nav aria-label="장소 유형 바로가기" class="grid grid-cols-3 sm:grid-cols-6 gap-3">
      <router-link
        v-for="item in SHORTCUTS"
        :key="item.category"
        :to="{ path: '/search', query: { category: item.category } }"
        class="flex flex-col items-center gap-1.5 bg-white border border-stone-100 rounded-2xl py-3 text-sm font-medium text-stone-700 hover:border-mint-200 hover:bg-mint-50 transition"
      >
        <i :class="[item.icon, 'text-2xl text-mint-600']" aria-hidden="true"></i>
        <span class="whitespace-nowrap">{{ item.label }}</span>
      </router-link>
    </nav>

    <!-- 추천 동반 장소 -->
    <div class="space-y-4">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-bold text-stone-800 flex items-center gap-2">
          <i class="ri-sparkles-line text-amber-500"></i>
          <span>추천 동반 장소</span>
        </h2>
        <button @click="fetchBackendData" class="shrink-0 whitespace-nowrap px-2 py-3 -my-2 text-xs font-medium text-amber-600 hover:underline">
          새로고침
        </button>
      </div>

      <LoadingSpinner v-if="loading" text="추천 장소를 불러오는 중입니다..." />

      <ErrorMessage v-else-if="errorMessage" :message="errorMessage" illustrated />

      <div v-else-if="places.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <BaseCard v-for="place in places" :key="place.id" hoverable @click="$router.push(`/place/${place.id}`)">
          <div class="h-40 bg-stone-100 rounded-xl overflow-hidden mb-3 relative">
            <PlaceImage
              :image-url="place.imageUrl"
              :thumbnail-url="place.thumbnailUrl"
              :prefer-thumbnail="false"
              :alt="place.name"
              :category="place.category"
              :copyright-type="place.imageCopyrightType"
            />
            <span class="absolute top-2 left-2 bg-white/90 backdrop-blur-sm text-stone-700 text-xs font-bold px-2.5 py-1 rounded-lg">
              {{ place.categoryName || place.category }}
            </span>
          </div>

          <h3 class="font-bold text-stone-800 text-base truncate">{{ place.name }}</h3>
          <p class="text-xs text-stone-500 truncate mt-1">{{ place.address }}</p>

          <div class="mt-3 pt-3 border-t border-stone-100 flex items-center justify-between text-xs">
            <div class="flex items-center gap-1 text-amber-500 font-bold">
              <i class="ri-star-fill"></i>
              <span>{{ formatRating(place.rating, place.reviewCount) }}</span>
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
