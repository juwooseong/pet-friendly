<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { formatRating } from '@/utils/rating';
import { imageFitClass } from '@/utils/publicData';
import { useRouter } from 'vue-router';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import { useAuthStore } from '@/stores/authStore';
import type { ApiResponse } from '@/types/api';
import type { FavoriteItem } from '@/types/favorite';
import BaseCard from '@/components/common/BaseCard.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const router = useRouter();
const authStore = useAuthStore();

const favorites = ref<FavoriteItem[]>([]);
const loading = ref(true);
const errorMessage = ref('');
const removingPlaceId = ref<string | null>(null);
const removeErrorMessage = ref('');

const fetchFavorites = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await apiClient.get<ApiResponse<FavoriteItem[]>>('/users/me/favorites');
    if (response.data && response.data.success) {
      favorites.value = Array.isArray(response.data.data) ? response.data.data : [];
    }
  } catch (err) {
    errorMessage.value = extractErrorMessage(err, '즐겨찾기 목록을 불러오는 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
};

const goToPlaceDetail = (placeId: string) => {
  router.push({ name: 'PlaceDetail', params: { id: placeId } });
};

/** 삭제 버튼 클릭 시 카드 클릭(상세 이동) 이벤트로 전파되지 않도록 별도 핸들러로 분리 */
const removeFavorite = async (placeId: string) => {
  if (removingPlaceId.value) return;

  removingPlaceId.value = placeId;
  removeErrorMessage.value = '';
  try {
    await apiClient.delete(`/favorites/${placeId}`);
    favorites.value = favorites.value.filter((fav) => fav.placeId !== placeId);
  } catch (err) {
    removeErrorMessage.value = extractErrorMessage(err, '즐겨찾기 삭제 중 오류가 발생했습니다.');
  } finally {
    removingPlaceId.value = null;
  }
};

onMounted(async () => {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'Login', query: { redirect: '/favorites' } });
    return;
  }
  await fetchFavorites();
});
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold text-stone-800">⭐ 내 즐겨찾기 장소</h1>

    <LoadingSpinner v-if="loading" text="즐겨찾기 목록 불러오는 중..." />

    <ErrorMessage v-else-if="errorMessage" :message="errorMessage" />

    <EmptyState
      v-else-if="favorites.length === 0"
      icon="ri-heart-line"
      title="등록된 즐겨찾기가 없습니다."
      description="마음에 드는 장소를 즐겨찾기에 추가해보세요!"
    />

    <template v-else>
      <ErrorMessage v-if="removeErrorMessage" :message="removeErrorMessage" />

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <BaseCard
          v-for="fav in favorites"
          :key="fav.favoriteId"
          hoverable
          @click="goToPlaceDetail(fav.placeId)"
        >
          <div class="h-36 -mx-5 -mt-5 mb-3 bg-stone-100 rounded-t-2xl overflow-hidden">
            <img
              v-if="fav.imageUrl"
              :src="fav.imageUrl"
              :alt="fav.placeName"
              :class="['w-full h-full', imageFitClass(fav.imageCopyrightType)]"
              @error="($event.target as HTMLImageElement).style.display = 'none'"
            />
            <div v-else class="w-full h-full flex items-center justify-center text-stone-300">
              <i class="ri-image-line text-3xl"></i>
            </div>
          </div>

          <span class="bg-amber-100 text-amber-700 text-xs font-bold px-2.5 py-1 rounded-lg">
            {{ fav.categoryName }}
          </span>
          <h3 class="font-bold text-stone-800 mt-2">{{ fav.placeName }}</h3>
          <p class="text-xs text-stone-500 mt-1">{{ fav.address }}</p>

          <div class="flex items-center justify-between mt-3">
            <div class="text-amber-500 font-bold text-sm">
              ★ {{ formatRating(fav.rating, fav.reviewCount) }}
              <span class="text-stone-400 font-normal">리뷰 {{ fav.reviewCount }}개</span>
            </div>
            <button
              type="button"
              :disabled="removingPlaceId === fav.placeId"
              class="flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-xs font-bold bg-rose-50 text-rose-500 border border-rose-200 hover:bg-rose-100 transition disabled:opacity-50 disabled:cursor-not-allowed"
              @click.stop="removeFavorite(fav.placeId)"
            >
              <i class="ri-delete-bin-line"></i>
              <span>삭제</span>
            </button>
          </div>
        </BaseCard>
      </div>
    </template>
  </div>
</template>
