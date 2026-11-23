<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { extractErrorMessage } from '@/api/apiClient';
import { useAuthStore } from '@/stores/authStore';
import { usePetStore } from '@/stores/petStore';
import type { Pet, PetCreateRequest, PetGender } from '@/types/user';
import BaseCard from '@/components/common/BaseCard.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseModal from '@/components/common/BaseModal.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const router = useRouter();
const authStore = useAuthStore();
const petStore = usePetStore();
const { pets } = storeToRefs(petStore);

const loading = ref(true);
const errorMessage = ref('');

const genderLabel = (gender: PetGender): string => (gender === 'MALE' ? '남아' : '여아');
const sizeLabel = (size: string): string => {
  if (size === 'SMALL') return '소형견';
  if (size === 'MEDIUM') return '중형견';
  if (size === 'LARGE') return '대형견';
  return size;
};
const formatBirthDate = (birthDate: string | null): string => {
  if (!birthDate) return '생년월일 미등록';
  return birthDate;
};

const fetchPets = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    // 관리 화면은 들어올 때마다 최신 목록을 받는다
    await petStore.fetchPets();
  } catch (err) {
    errorMessage.value = extractErrorMessage(err, '반려동물 목록을 불러오는 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
};

/** 등록/수정 공용 폼 모달 상태 */
const formMode = ref<'create' | 'edit' | null>(null);
const editingPetId = ref<string | null>(null);
const form = reactive({
  name: '',
  breed: '',
  gender: 'MALE' as PetGender,
  birthDate: '',
  weightKg: '',
  neutered: false,
  photoUrl: '',
});
const formValidationMessage = ref('');
const formSubmitErrorMessage = ref('');
const formSubmitLoading = ref(false);

const resetForm = () => {
  form.name = '';
  form.breed = '';
  form.gender = 'MALE';
  form.birthDate = '';
  form.weightKg = '';
  form.neutered = false;
  form.photoUrl = '';
  formValidationMessage.value = '';
  formSubmitErrorMessage.value = '';
};

const openCreateForm = () => {
  resetForm();
  editingPetId.value = null;
  formMode.value = 'create';
};

const openEditForm = (pet: Pet) => {
  form.name = pet.name;
  form.breed = pet.breed;
  form.gender = pet.gender;
  form.birthDate = pet.birthDate || '';
  form.weightKg = String(pet.weightKg);
  form.neutered = pet.neutered;
  form.photoUrl = pet.photoUrl || '';
  formValidationMessage.value = '';
  formSubmitErrorMessage.value = '';
  editingPetId.value = pet.id;
  formMode.value = 'edit';
};

const closeForm = () => {
  if (formSubmitLoading.value) return;
  formMode.value = null;
  editingPetId.value = null;
};

const submitForm = async () => {
  formValidationMessage.value = '';
  formSubmitErrorMessage.value = '';

  const weight = Number(form.weightKg);
  if (!form.name.trim() || !form.breed.trim() || !form.gender || !form.weightKg || Number.isNaN(weight) || weight <= 0) {
    formValidationMessage.value = '이름, 품종, 성별, 체중(0보다 큰 값)을 모두 입력해주세요.';
    return;
  }
  if (formSubmitLoading.value) return;

  const payload: PetCreateRequest = {
    name: form.name.trim(),
    breed: form.breed.trim(),
    gender: form.gender,
    birthDate: form.birthDate || null,
    weightKg: weight,
    neutered: form.neutered,
    photoUrl: form.photoUrl.trim() || null,
  };

  formSubmitLoading.value = true;
  try {
    if (formMode.value === 'edit' && editingPetId.value) {
      await petStore.updatePet(editingPetId.value, payload);
    } else {
      await petStore.createPet(payload);
    }
    formMode.value = null;
    editingPetId.value = null;
  } catch (err) {
    formSubmitErrorMessage.value = extractErrorMessage(
      err,
      formMode.value === 'edit' ? '반려동물 정보 수정 중 오류가 발생했습니다.' : '반려동물 등록 중 오류가 발생했습니다.'
    );
  } finally {
    formSubmitLoading.value = false;
  }
};

/** 삭제 확인 모달 상태 */
const deletingPet = ref<Pet | null>(null);
const deleteLoading = ref(false);
const deleteErrorMessage = ref('');

const openDeleteConfirm = (pet: Pet) => {
  deleteErrorMessage.value = '';
  deletingPet.value = pet;
};

const closeDeleteConfirm = () => {
  if (deleteLoading.value) return;
  deletingPet.value = null;
};

const confirmDelete = async () => {
  if (!deletingPet.value || deleteLoading.value) return;

  deleteLoading.value = true;
  deleteErrorMessage.value = '';
  try {
    await petStore.deletePet(deletingPet.value.id);
    deletingPet.value = null;
  } catch (err) {
    deleteErrorMessage.value = extractErrorMessage(err, '반려동물 삭제 중 오류가 발생했습니다.');
  } finally {
    deleteLoading.value = false;
  }
};

/** 대표 반려동물 변경 */
const representativeChangingId = ref<string | null>(null);
const representativeErrorMessage = ref('');

const setRepresentative = async (pet: Pet) => {
  if (pet.representative || representativeChangingId.value) return;

  representativeChangingId.value = pet.id;
  representativeErrorMessage.value = '';
  try {
    await petStore.setRepresentative(pet.id);
  } catch (err) {
    representativeErrorMessage.value = extractErrorMessage(err, '대표 반려동물 변경 중 오류가 발생했습니다.');
  } finally {
    representativeChangingId.value = null;
  }
};

const formTitle = computed(() => (formMode.value === 'edit' ? '반려동물 정보 수정' : '반려동물 등록'));

onMounted(async () => {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'Login', query: { redirect: '/pets' } });
    return;
  }
  await fetchPets();
});
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center gap-3">
      <h1 class="text-xl sm:text-2xl font-bold text-stone-800">🐾 내 반려동물 관리</h1>
      <BaseButton size="sm" @click="openCreateForm">+ 반려동물 등록</BaseButton>
    </div>

    <LoadingSpinner v-if="loading" text="반려동물 정보 로딩 중..." />

    <ErrorMessage v-else-if="errorMessage" :message="errorMessage" />

    <EmptyState
      v-else-if="pets.length === 0"
      icon="ri-footprint-line"
      title="등록된 반려동물이 없습니다."
      description="첫 반려동물을 등록하면 자동으로 대표 반려동물로 지정됩니다."
    />

    <template v-else>
      <ErrorMessage v-if="representativeErrorMessage" :message="representativeErrorMessage" />

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <BaseCard v-for="pet in pets" :key="pet.id">
          <div class="flex items-start gap-3">
            <div class="w-12 h-12 shrink-0 bg-amber-100 text-amber-600 rounded-full flex items-center justify-center font-bold text-lg overflow-hidden">
              <img v-if="pet.photoUrl" :src="pet.photoUrl" :alt="pet.name" class="w-full h-full object-cover" />
              <span v-else>🐶</span>
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-1.5 flex-wrap">
                <h3 class="font-bold text-stone-800">{{ pet.name }} ({{ pet.breed }})</h3>
                <span
                  v-if="pet.representative"
                  class="text-xs font-bold px-2 py-0.5 rounded-full bg-amber-500 text-white shrink-0"
                >
                  대표
                </span>
              </div>
              <p class="text-xs text-stone-500 mt-1">{{ pet.weightKg }}kg · {{ sizeLabel(pet.sizeCategory) }}</p>
              <p class="text-xs text-stone-500">{{ genderLabel(pet.gender) }} · {{ pet.neutered ? '중성화 완료' : '중성화 안 함' }}</p>
              <p class="text-xs text-stone-400">{{ formatBirthDate(pet.birthDate) }}</p>
            </div>
          </div>

          <div class="flex flex-wrap gap-2 mt-4 pt-4 border-t border-stone-100">
            <BaseButton
              v-if="!pet.representative"
              variant="outline"
              size="sm"
              :disabled="representativeChangingId === pet.id"
              @click="setRepresentative(pet)"
            >
              대표로 설정
            </BaseButton>
            <BaseButton variant="secondary" size="sm" @click="openEditForm(pet)">수정</BaseButton>
            <BaseButton variant="danger" size="sm" @click="openDeleteConfirm(pet)">삭제</BaseButton>
          </div>
        </BaseCard>
      </div>
    </template>

    <BaseModal :is-open="formMode !== null" :title="formTitle" @close="closeForm">
      <div class="flex flex-col gap-3">
        <BaseInput v-model="form.name" label="이름" placeholder="예: 초코" :disabled="formSubmitLoading" />
        <BaseInput v-model="form.breed" label="품종" placeholder="예: 골든리트리버" :disabled="formSubmitLoading" />

        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-semibold text-stone-700">성별</label>
          <select
            v-model="form.gender"
            :disabled="formSubmitLoading"
            class="px-3.5 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm text-stone-800 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:bg-white transition disabled:bg-stone-100 disabled:cursor-not-allowed"
          >
            <option value="MALE">남아</option>
            <option value="FEMALE">여아</option>
          </select>
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-semibold text-stone-700">생년월일</label>
          <input
            v-model="form.birthDate"
            type="date"
            :disabled="formSubmitLoading"
            class="px-3.5 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm text-stone-800 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:bg-white transition disabled:bg-stone-100 disabled:cursor-not-allowed"
          />
        </div>

        <BaseInput v-model="form.weightKg" type="number" label="체중(kg)" placeholder="예: 5.5" :disabled="formSubmitLoading" />

        <label class="flex items-center gap-2 text-sm text-stone-700">
          <input v-model="form.neutered" type="checkbox" :disabled="formSubmitLoading" class="w-4 h-4 accent-amber-500" />
          중성화 완료
        </label>

        <ErrorMessage v-if="formValidationMessage" :message="formValidationMessage" />
        <ErrorMessage v-if="formSubmitErrorMessage" :message="formSubmitErrorMessage" />
      </div>

      <template #footer>
        <BaseButton variant="secondary" :disabled="formSubmitLoading" @click="closeForm">취소</BaseButton>
        <BaseButton :loading="formSubmitLoading" @click="submitForm">
          {{ formMode === 'edit' ? '수정 완료' : '등록' }}
        </BaseButton>
      </template>
    </BaseModal>

    <BaseModal :is-open="deletingPet !== null" title="반려동물 삭제" @close="closeDeleteConfirm">
      <div class="flex flex-col gap-3">
        <p class="text-sm text-stone-600">
          <strong>{{ deletingPet?.name }}</strong>을(를) 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
        </p>
        <ErrorMessage v-if="deleteErrorMessage" :message="deleteErrorMessage" />
      </div>

      <template #footer>
        <BaseButton variant="secondary" :disabled="deleteLoading" @click="closeDeleteConfirm">취소</BaseButton>
        <BaseButton variant="danger" :loading="deleteLoading" @click="confirmDelete">삭제</BaseButton>
      </template>
    </BaseModal>
  </div>
</template>
