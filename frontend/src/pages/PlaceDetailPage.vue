<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import { useAuthStore } from '@/stores/authStore';
import type { ApiResponse } from '@/types/api';
import type { PlaceSearchItem } from '@/types/place';
import type { FavoriteItem } from '@/types/favorite';
import type { Review } from '@/types/review';
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
const imageLoadError = ref(false);

/**
 * 반려동물 동반 수칙 섹션에 표시할 정보 존재 여부.
 * 현재 API(`PlaceSearchResponseDto`)가 제공하는 필드(allowedSizes, maxWeightLimitKg)만 사용하며,
 * 엔티티에는 존재하지만 API 응답에 아직 노출되지 않은 isIndoorAllowed/isOutdoorAllowed/hasOffLeashZone은
 * 다루지 않는다(TSK-BE-031 백로그 참조).
 */
const hasPetPolicyInfo = computed(
  () => !!place.value && ((place.value.allowedSizes?.length ?? 0) > 0 || place.value.maxWeightLimitKg != null)
);

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
  imageLoadError.value = false;
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

const reviews = ref<Review[]>([]);
const reviewsLoading = ref(true);
const reviewsErrorMessage = ref('');

const newRating = ref(0);
const newContent = ref('');
const reviewSubmitLoading = ref(false);
const reviewSubmitErrorMessage = ref('');
const reviewFormValidationMessage = ref('');

const formatReviewDate = (createdAt: string): string => {
  const date = new Date(createdAt);
  if (Number.isNaN(date.getTime())) return createdAt;
  return date.toLocaleDateString('ko-KR');
};

const fetchReviews = async () => {
  reviewsLoading.value = true;
  reviewsErrorMessage.value = '';
  try {
    const response = await apiClient.get<ApiResponse<Review[]>>(`/places/${placeId}/reviews`);
    if (response.data && response.data.success) {
      reviews.value = Array.isArray(response.data.data) ? response.data.data : [];
    }
  } catch (err) {
    reviewsErrorMessage.value = extractErrorMessage(err, '리뷰 목록을 불러오는 중 오류가 발생했습니다.');
  } finally {
    reviewsLoading.value = false;
  }
};

const goToLogin = () => {
  router.push({ name: 'Login', query: { redirect: route.fullPath } });
};

const submitReview = async () => {
  reviewFormValidationMessage.value = '';
  reviewSubmitErrorMessage.value = '';

  if (newRating.value < 1 || !newContent.value.trim()) {
    reviewFormValidationMessage.value = '평점과 리뷰 내용을 모두 입력해주세요.';
    return;
  }
  if (reviewSubmitLoading.value) return;

  reviewSubmitLoading.value = true;
  try {
    await apiClient.post('/reviews', {
      placeId,
      rating: newRating.value,
      content: newContent.value.trim(),
    });
    newRating.value = 0;
    newContent.value = '';
    await Promise.all([fetchReviews(), fetchPlaceDetail()]);
  } catch (err) {
    reviewSubmitErrorMessage.value = extractErrorMessage(err, '리뷰 등록 중 오류가 발생했습니다.');
  } finally {
    reviewSubmitLoading.value = false;
  }
};

onMounted(async () => {
  await fetchPlaceDetail();
  await fetchFavoriteStatus();
  await fetchReviews();
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
          v-if="place.imageUrl && !imageLoadError"
          :src="place.imageUrl"
          :alt="place.name"
          class="w-full h-full object-cover"
          @error="imageLoadError = true"
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

      <div class="pt-4 border-t border-stone-100 space-y-2">
        <h3 class="text-sm font-bold text-stone-700 flex items-center gap-1.5">
          <i class="ri-shield-check-line text-amber-500"></i>
          <span>반려동물 동반 수칙</span>
        </h3>
        <template v-if="hasPetPolicyInfo">
          <div v-if="place.allowedSizes && place.allowedSizes.length > 0" class="flex flex-wrap gap-1.5">
            <span
              v-for="size in place.allowedSizes"
              :key="size"
              class="text-xs font-bold px-2.5 py-1 rounded-full bg-amber-50 text-amber-600 border border-amber-100"
            >
              {{ sizeLabel(size) }}
            </span>
          </div>
          <p v-if="place.maxWeightLimitKg != null" class="text-sm text-stone-600 flex items-center gap-2">
            <i class="ri-scales-3-line text-amber-500"></i>
            <span>최대 체중 {{ place.maxWeightLimitKg }}kg까지 동반 가능</span>
          </p>
        </template>
        <p v-else class="text-sm text-stone-400">등록된 반려동물 동반 수칙 정보가 없습니다.</p>
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

    <div v-if="place" class="bg-white p-8 rounded-3xl border border-stone-100 shadow-sm space-y-6">
      <h2 class="text-xl font-extrabold text-stone-800">리뷰</h2>

      <div v-if="authStore.isAuthenticated" class="space-y-3 p-4 rounded-2xl bg-stone-50 border border-stone-100">
        <div class="flex items-center gap-1">
          <button
            v-for="star in 5"
            :key="star"
            type="button"
            @click="newRating = star"
            class="text-2xl"
            :class="star <= newRating ? 'text-amber-500' : 'text-stone-300'"
          >
            ★
          </button>
        </div>
        <textarea
          v-model="newContent"
          maxlength="1000"
          rows="3"
          placeholder="이 장소에 대한 리뷰를 남겨주세요."
          class="w-full rounded-xl border border-stone-200 p-3 text-sm text-stone-700 focus:outline-none focus:ring-2 focus:ring-amber-300"
        ></textarea>
        <ErrorMessage v-if="reviewFormValidationMessage" :message="reviewFormValidationMessage" />
        <ErrorMessage v-if="reviewSubmitErrorMessage" :message="reviewSubmitErrorMessage" />
        <button
          type="button"
          @click="submitReview"
          :disabled="reviewSubmitLoading"
          class="px-4 py-2 rounded-xl text-sm font-bold bg-amber-500 text-white hover:bg-amber-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          리뷰 등록
        </button>
      </div>
      <div v-else class="p-4 rounded-2xl bg-stone-50 border border-stone-100 text-sm text-stone-600 flex items-center justify-between gap-3">
        <span>리뷰를 작성하려면 로그인이 필요합니다.</span>
        <button
          type="button"
          @click="goToLogin"
          class="px-3 py-1.5 rounded-xl text-sm font-bold bg-amber-500 text-white hover:bg-amber-600 transition shrink-0"
        >
          로그인
        </button>
      </div>

      <LoadingSpinner v-if="reviewsLoading" text="리뷰 로딩 중..." />

      <ErrorMessage v-else-if="reviewsErrorMessage" :message="reviewsErrorMessage" />

      <EmptyState
        v-else-if="reviews.length === 0"
        icon="ri-chat-3-line"
        title="아직 작성된 리뷰가 없습니다."
        description="가장 먼저 리뷰를 남겨보세요!"
      />

      <div v-else class="space-y-4">
        <div
          v-for="review in reviews"
          :key="review.id"
          class="p-4 rounded-2xl border border-stone-100 space-y-1.5"
        >
          <div class="flex items-center justify-between">
            <span class="font-bold text-stone-700 text-sm">{{ review.userNickname }}</span>
            <span class="text-amber-500 text-sm font-bold">{{ '★'.repeat(review.rating) }}</span>
          </div>
          <p class="text-stone-600 text-sm whitespace-pre-wrap">{{ review.content }}</p>
          <p class="text-stone-400 text-xs">{{ formatReviewDate(review.createdAt) }}</p>
        </div>
      </div>
    </div>
  </div>
</template>
