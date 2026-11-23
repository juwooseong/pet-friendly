<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import { useAuthStore } from '@/stores/authStore';
import { isValidPassword, doPasswordsMatch } from '@/utils/validators';

/**
 * 비밀번호 재설정 (TSK-SEC-003). 비밀번호 찾기 메일의 링크(`/reset-password?token=...`)로 들어온다.
 * 링크는 1회용·30분이며, 재설정하면 이 계정의 기존 로그인이 모두 끊긴다.
 */
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const token = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const newPasswordError = ref('');
const confirmPasswordError = ref('');
const errorMessage = ref('');
const loading = ref(false);
const completed = ref(false);

onMounted(() => {
  const value = route.query.token;
  token.value = typeof value === 'string' ? value : '';
  // 토큰이 주소창·방문 기록에 남지 않도록 주소에서 지운다 (화면은 메모리의 값을 쓴다)
  if (value !== undefined) {
    router.replace({ path: route.path, query: {} });
  }
});

const handleSubmit = async () => {
  newPasswordError.value = '';
  confirmPasswordError.value = '';
  errorMessage.value = '';

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
    await apiClient.post('/auth/reset-password', {
      token: token.value,
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value,
    });
    completed.value = true;
    newPassword.value = '';
    confirmPassword.value = '';
    // 이 브라우저에 남아 있던 로그인 토큰은 서버에서 무효화되었으므로 정리한다
    if (authStore.isAuthenticated) {
      authStore.logout();
    }
  } catch (err: unknown) {
    errorMessage.value = extractErrorMessage(err, '비밀번호 재설정 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="space-y-6">
    <div class="text-center">
      <h2 class="text-2xl font-bold text-stone-800">비밀번호 재설정 🔑</h2>
      <p class="text-xs text-stone-500 mt-1">새로 사용할 비밀번호를 입력해주세요.</p>
    </div>

    <div v-if="completed" class="bg-amber-50 border border-amber-200 rounded-xl p-4 text-center text-sm text-stone-700 space-y-1">
      <p>비밀번호가 변경되었습니다.</p>
      <p class="text-xs text-stone-500">다른 기기의 로그인도 모두 해제되었어요. 새 비밀번호로 로그인해주세요.</p>
    </div>

    <div v-else-if="!token" class="bg-stone-50 border border-stone-200 rounded-xl p-4 text-center text-sm text-stone-700 space-y-1">
      <p>재설정 링크가 올바르지 않습니다.</p>
      <p class="text-xs text-stone-500">메일의 링크를 다시 눌러 주시거나, 비밀번호 찾기를 다시 요청해주세요.</p>
    </div>

    <template v-else>
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
    </template>

    <div class="text-center text-xs text-stone-500 pt-2 border-t border-stone-100 flex justify-center gap-4">
      <router-link to="/login" class="text-amber-600 font-bold hover:underline">로그인</router-link>
      <span class="text-stone-300">|</span>
      <router-link to="/find-password" class="text-amber-600 font-bold hover:underline">비밀번호 찾기 다시 요청</router-link>
    </div>
  </div>
</template>
