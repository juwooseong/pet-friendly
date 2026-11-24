<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import { useAuthStore } from '@/stores/authStore';
import type { ApiResponse } from '@/types/api';
import type { Review } from '@/types/review';
import BaseCard from '@/components/common/BaseCard.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const router = useRouter();
const authStore = useAuthStore();

const reviews = ref<Review[]>([]);
const loading = ref(true);
const errorMessage = ref('');

const formatReviewDate = (createdAt: string): string => {
  const date = new Date(createdAt);
  if (Number.isNaN(date.getTime())) return createdAt;
  return date.toLocaleDateString('ko-KR');
};

const fetchMyReviews = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await apiClient.get<ApiResponse<Review[]>>('/users/me/reviews');
    if (response.data && response.data.success) {
      reviews.value = Array.isArray(response.data.data) ? response.data.data : [];
    }
  } catch (err) {
    errorMessage.value = extractErrorMessage(err, '내 리뷰 목록을 불러오는 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
};

const goToPlaceDetail = (placeId: string) => {
  router.push({ name: 'PlaceDetail', params: { id: placeId } });
};

onMounted(async () => {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'Login', query: { redirect: '/mypage/reviews' } });
    return;
  }
  await fetchMyReviews();
});
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold text-stone-800">✍️ 내가 작성한 리뷰</h1>

    <LoadingSpinner v-if="loading" text="리뷰 목록 불러오는 중..." />

    <ErrorMessage v-else-if="errorMessage" :message="errorMessage" illustrated />

    <EmptyState
      v-else-if="reviews.length === 0"
      character="nabi-review"
      title="아직 작성한 리뷰가 없습니다."
      description="장소 상세 페이지에서 리뷰를 남겨보세요!"
    />

    <div v-else class="space-y-4">
      <BaseCard v-for="review in reviews" :key="review.id" hoverable @click="goToPlaceDetail(review.placeId)">
        <div class="flex gap-3">
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between gap-2">
              <span class="font-bold text-stone-800">{{ review.placeName }}</span>
              <span class="text-amber-500 text-sm font-bold whitespace-nowrap">{{ '★'.repeat(review.rating) }}</span>
            </div>
            <p class="text-stone-600 text-sm mt-1 whitespace-pre-wrap">{{ review.content }}</p>
            <p class="text-stone-400 text-xs mt-2">{{ formatReviewDate(review.createdAt) }}</p>
          </div>
          <!-- 리뷰 사진은 썸네일만 (카드를 누르면 장소 상세로 이동) -->
          <img
            v-if="review.imageUrl"
            :src="review.thumbnailUrl ?? review.imageUrl"
            alt="리뷰 사진"
            loading="lazy"
            class="shrink-0 w-16 h-16 rounded-xl object-cover border border-stone-100"
            data-testid="review-thumbnail"
          />
        </div>
      </BaseCard>
    </div>
  </div>
</template>
