<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import { isValidPassword, doPasswordsMatch } from '@/utils/validators';

const router = useRouter();
const authStore = useAuthStore();

const newPassword = ref('');
const confirmPassword = ref('');
const loading = ref(false);
const errorMessage = ref('');
const newPasswordError = ref('');
const confirmPasswordError = ref('');

const handleSubmit = async () => {
  newPasswordError.value = '';
  confirmPasswordError.value = '';
  errorMessage.value = '';

  if (!newPassword.value || !confirmPassword.value) {
    errorMessage.value = '새 비밀번호와 비밀번호 확인을 모두 입력해주세요.';
    return;
  }

  if (!isValidPassword(newPassword.value)) {
    newPasswordError.value = '비밀번호는 8자 이상 100자 이하이며 영문/숫자/특수문자를 모두 포함해야 합니다.';
    return;
  }

  if (!doPasswordsMatch(newPassword.value, confirmPassword.value)) {
    confirmPasswordError.value = '비밀번호가 일치하지 않습니다.';
    return;
  }

  loading.value = true;

  try {
    const response = await apiClient.patch('/auth/password', {
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value,
    });

    if (response.data && response.data.success) {
      authStore.completePasswordChange();
      newPassword.value = '';
      confirmPassword.value = '';
      alert('비밀번호가 변경되었습니다.');
      router.push('/');
    } else {
      errorMessage.value = response.data.error || '비밀번호 변경에 실패했습니다.';
    }
  } catch (err: any) {
    errorMessage.value = extractErrorMessage(err, '비밀번호 변경 처리 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm">
    <div class="w-full max-w-md bg-white rounded-2xl shadow-xl border border-stone-100 p-6 sm:p-8 space-y-6">
      <div class="text-center">
        <h2 class="text-xl font-bold text-stone-800">🔒 비밀번호 변경이 필요해요</h2>
        <p class="text-xs text-stone-500 mt-2 leading-relaxed">
          임시 비밀번호로 로그인하셨습니다.<br />
          서비스 이용을 위해 새로운 비밀번호를 설정해주세요.
        </p>
      </div>

      <ErrorMessage v-if="errorMessage" :message="errorMessage" />

      <form @submit.prevent="handleSubmit" class="space-y-4">
        <BaseInput
          v-model="newPassword"
          label="새 비밀번호"
          placeholder="8자 이상 영문/숫자/특수문자"
          type="password"
          :error="newPasswordError"
          @update:modelValue="newPasswordError = ''"
        />

        <BaseInput
          v-model="confirmPassword"
          label="새 비밀번호 확인"
          placeholder="비밀번호를 다시 입력해주세요"
          type="password"
          :error="confirmPasswordError"
          @update:modelValue="confirmPasswordError = ''"
        />

        <BaseButton type="submit" variant="primary" size="lg" :loading="loading" class="w-full mt-2">
          비밀번호 변경
        </BaseButton>
      </form>
    </div>
  </div>
</template>
