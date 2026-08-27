<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import { useAuthStore } from '@/stores/authStore';
import type { ApiResponse } from '@/types/api';
import type { MyPageSummary, UserProfileResponse } from '@/types/user';
import BaseCard from '@/components/common/BaseCard.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseModal from '@/components/common/BaseModal.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const router = useRouter();
const authStore = useAuthStore();

const summary = ref<MyPageSummary | null>(null);
const loading = ref(true);
const errorMessage = ref('');

const sizeLabel = (size: string): string => {
  if (size === 'SMALL') return '소형견';
  if (size === 'MEDIUM') return '중형견';
  if (size === 'LARGE') return '대형견';
  return size;
};

const fetchMyPage = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await apiClient.get<ApiResponse<MyPageSummary>>('/users/me');
    if (response.data && response.data.success) {
      summary.value = response.data.data;
    }
  } catch (err) {
    errorMessage.value = extractErrorMessage(err, '마이페이지 정보를 불러오는 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
};

const goToFavorites = () => router.push({ name: 'Favorites' });
const goToPets = () => router.push({ name: 'Pets' });
const goToMyReviews = () => router.push({ name: 'MyReviews' });

/** 닉네임/프로필 이미지 수정 모달 상태 */
const isEditOpen = ref(false);
const editForm = reactive({ nickname: '', avatarUrl: '' });
const editValidationMessage = ref('');
const editSubmitErrorMessage = ref('');
const editSubmitLoading = ref(false);

const openEditProfile = () => {
  if (!summary.value) return;
  editForm.nickname = summary.value.nickname;
  editForm.avatarUrl = summary.value.avatarUrl || '';
  editValidationMessage.value = '';
  editSubmitErrorMessage.value = '';
  isEditOpen.value = true;
};

const closeEditProfile = () => {
  if (editSubmitLoading.value) return;
  isEditOpen.value = false;
};

const submitEditProfile = async () => {
  editValidationMessage.value = '';
  editSubmitErrorMessage.value = '';

  if (!editForm.nickname.trim() || editForm.nickname.trim().length < 2) {
    editValidationMessage.value = '닉네임은 2자 이상 입력해주세요.';
    return;
  }
  if (editSubmitLoading.value) return;

  editSubmitLoading.value = true;
  try {
    const response = await apiClient.put<ApiResponse<UserProfileResponse>>('/users/me', {
      nickname: editForm.nickname.trim(),
      avatarUrl: editForm.avatarUrl.trim() || null,
    });
    const updated = response.data?.data;
    if (updated && authStore.currentUser) {
      authStore.setUser({
        ...authStore.currentUser,
        nickname: updated.nickname,
        avatarUrl: updated.avatarUrl || undefined,
      });
    }
    isEditOpen.value = false;
    await fetchMyPage();
  } catch (err) {
    editSubmitErrorMessage.value = extractErrorMessage(err, '프로필 수정 중 오류가 발생했습니다.');
  } finally {
    editSubmitLoading.value = false;
  }
};

onMounted(async () => {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'Login', query: { redirect: '/mypage' } });
    return;
  }
  await fetchMyPage();
});
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold text-stone-800">👤 마이페이지</h1>

    <LoadingSpinner v-if="loading" text="마이페이지 정보 불러오는 중..." />

    <ErrorMessage v-else-if="errorMessage" :message="errorMessage" />

    <template v-else-if="summary">
      <div class="bg-white p-6 rounded-2xl border border-stone-100 shadow-sm space-y-4">
        <div class="flex items-center justify-between gap-4 flex-wrap">
          <div class="flex items-center gap-4">
            <div class="w-16 h-16 shrink-0 bg-amber-500 text-white rounded-full flex items-center justify-center font-bold text-2xl overflow-hidden">
              <img v-if="summary.avatarUrl" :src="summary.avatarUrl" :alt="summary.nickname" class="w-full h-full object-cover" />
              <span v-else>{{ summary.nickname?.charAt(0) || 'U' }}</span>
            </div>
            <div>
              <h2 class="text-xl font-bold text-stone-800">{{ summary.nickname }}</h2>
              <p class="text-xs text-stone-500">{{ summary.email }}</p>
            </div>
          </div>
          <BaseButton variant="outline" size="sm" @click="openEditProfile">내 정보 수정</BaseButton>
        </div>

        <div class="grid grid-cols-3 gap-4 pt-4 border-t border-stone-100 text-center">
          <button
            type="button"
            class="bg-stone-50 hover:bg-stone-100 p-3 rounded-xl transition"
            @click="goToPets"
          >
            <div class="text-xs text-stone-500">반려동물</div>
            <div class="text-lg font-bold text-amber-600">{{ summary.petCount }}마리</div>
          </button>
          <button
            type="button"
            class="bg-stone-50 hover:bg-stone-100 p-3 rounded-xl transition"
            @click="goToFavorites"
          >
            <div class="text-xs text-stone-500">즐겨찾기</div>
            <div class="text-lg font-bold text-amber-600">{{ summary.favoriteCount }}개</div>
          </button>
          <button
            type="button"
            class="bg-stone-50 hover:bg-stone-100 p-3 rounded-xl transition"
            @click="goToMyReviews"
          >
            <div class="text-xs text-stone-500">작성 리뷰</div>
            <div class="text-lg font-bold text-amber-600">{{ summary.reviewCount }}개</div>
          </button>
        </div>
      </div>

      <BaseCard>
        <h3 class="font-bold text-stone-800 mb-3">🐾 대표 반려동물</h3>
        <div v-if="summary.representativePet" class="flex items-center gap-3">
          <div class="w-12 h-12 shrink-0 bg-amber-100 text-amber-600 rounded-full flex items-center justify-center font-bold text-lg">
            🐶
          </div>
          <div>
            <p class="font-bold text-stone-800">{{ summary.representativePet.petName }} ({{ summary.representativePet.breed }})</p>
            <p class="text-xs text-stone-500 mt-0.5">
              {{ summary.representativePet.weightKg }}kg · {{ sizeLabel(summary.representativePet.sizeCategory) }}
            </p>
          </div>
        </div>
        <EmptyState
          v-else
          icon="ri-footprint-line"
          title="등록된 대표 반려동물이 없습니다."
          description="반려동물을 등록하면 자동으로 대표 반려동물로 지정됩니다."
        />
      </BaseCard>

      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <BaseCard hoverable @click="goToFavorites">
          <div class="flex items-center gap-2 font-bold text-stone-800">
            <i class="ri-heart-line text-amber-500"></i>
            즐겨찾기
          </div>
          <p class="text-xs text-stone-500 mt-1">저장한 장소를 확인하세요.</p>
        </BaseCard>
        <BaseCard hoverable @click="goToMyReviews">
          <div class="flex items-center gap-2 font-bold text-stone-800">
            <i class="ri-chat-3-line text-amber-500"></i>
            내 리뷰
          </div>
          <p class="text-xs text-stone-500 mt-1">작성한 리뷰를 확인하세요.</p>
        </BaseCard>
        <BaseCard hoverable @click="goToPets">
          <div class="flex items-center gap-2 font-bold text-stone-800">
            <i class="ri-footprint-line text-amber-500"></i>
            내 반려동물
          </div>
          <p class="text-xs text-stone-500 mt-1">반려동물 정보를 관리하세요.</p>
        </BaseCard>
      </div>
    </template>

    <BaseModal :is-open="isEditOpen" title="내 정보 수정" @close="closeEditProfile">
      <div class="flex flex-col gap-3">
        <BaseInput v-model="editForm.nickname" label="닉네임" placeholder="닉네임" :disabled="editSubmitLoading" />
        <BaseInput v-model="editForm.avatarUrl" label="프로필 이미지 URL (선택)" placeholder="https://..." :disabled="editSubmitLoading" />
        <ErrorMessage v-if="editValidationMessage" :message="editValidationMessage" />
        <ErrorMessage v-if="editSubmitErrorMessage" :message="editSubmitErrorMessage" />
      </div>

      <template #footer>
        <BaseButton variant="secondary" :disabled="editSubmitLoading" @click="closeEditProfile">취소</BaseButton>
        <BaseButton :loading="editSubmitLoading" @click="submitEditProfile">저장</BaseButton>
      </template>
    </BaseModal>
  </div>
</template>
