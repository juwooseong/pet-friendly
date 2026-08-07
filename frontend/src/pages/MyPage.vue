<script setup lang="ts">
import { ref, onMounted } from 'vue';
import apiClient from '@/api/apiClient';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';

const myInfo = ref<any>(null);
const loading = ref(true);

onMounted(async () => {
  try {
    const res = await apiClient.get('/users/me');
    if (res.data && res.data.success) {
      myInfo.value = res.data.data;
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
    <h1 class="text-2xl font-bold text-stone-800">👤 마이페이지</h1>
    <LoadingSpinner v-if="loading" text="마이페이지 정보 불러오는 중..." />
    <div v-else-if="myInfo" class="bg-white p-6 rounded-2xl border border-stone-100 shadow-sm space-y-4">
      <div class="flex items-center gap-4">
        <div class="w-16 h-16 bg-amber-500 text-white rounded-full flex items-center justify-center font-bold text-2xl">
          {{ myInfo.nickname?.charAt(0) || 'U' }}
        </div>
        <div>
          <h2 class="text-xl font-bold text-stone-800">{{ myInfo.nickname }}</h2>
          <p class="text-xs text-stone-500">{{ myInfo.email }}</p>
        </div>
      </div>

      <div class="grid grid-cols-3 gap-4 pt-4 border-t border-stone-100 text-center">
        <div class="bg-stone-50 p-3 rounded-xl">
          <div class="text-xs text-stone-500">반려동물</div>
          <div class="text-lg font-bold text-amber-600">{{ myInfo.petCount || 0 }}마리</div>
        </div>
        <div class="bg-stone-50 p-3 rounded-xl">
          <div class="text-xs text-stone-500">즐겨찾기</div>
          <div class="text-lg font-bold text-amber-600">{{ myInfo.favoriteCount || 0 }}개</div>
        </div>
        <div class="bg-stone-50 p-3 rounded-xl">
          <div class="text-xs text-stone-500">작성 리뷰</div>
          <div class="text-lg font-bold text-amber-600">{{ myInfo.reviewCount || 0 }}개</div>
        </div>
      </div>
    </div>
  </div>
</template>
