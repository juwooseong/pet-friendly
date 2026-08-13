<script setup lang="ts">
import { ref } from 'vue';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import apiClient, { extractErrorMessage } from '@/api/apiClient';

const email = ref('');
const loading = ref(false);
const errorMessage = ref('');
const submitted = ref(false);

const handleFindPassword = async () => {
  if (!email.value) {
    errorMessage.value = '이메일을 입력해주세요.';
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const response = await apiClient.post('/auth/find-password', {
      email: email.value,
    });

    if (response.data && response.data.success) {
      submitted.value = true;
    } else {
      errorMessage.value = response.data.error || '비밀번호 찾기에 실패했습니다.';
    }
  } catch (err: any) {
    errorMessage.value = extractErrorMessage(err, '비밀번호 찾기 처리 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="space-y-6">
    <div class="text-center">
      <h2 class="text-2xl font-bold text-stone-800">비밀번호 찾기 🔑</h2>
      <p class="text-xs text-stone-500 mt-1">가입하신 이메일로 임시 비밀번호를 보내드려요.</p>
    </div>

    <ErrorMessage v-if="errorMessage" :message="errorMessage" />

    <div v-if="submitted" class="bg-amber-50 border border-amber-200 rounded-xl p-4 text-center text-sm text-stone-700 space-y-1">
      <p>입력하신 이메일로 임시 비밀번호 안내를 처리했습니다.</p>
      <p class="text-xs text-stone-500">메일함을 확인한 뒤 임시 비밀번호로 로그인해주세요.</p>
    </div>

    <form v-else @submit.prevent="handleFindPassword" class="space-y-4">
      <BaseInput
        v-model="email"
        label="이메일 주소"
        placeholder="example@petspot.com"
        type="email"
      />

      <BaseButton type="submit" variant="primary" size="lg" :loading="loading" class="w-full mt-2">
        임시 비밀번호 받기
      </BaseButton>
    </form>

    <div class="text-center text-xs text-stone-500 pt-2 border-t border-stone-100 flex justify-center gap-4">
      <router-link to="/login" class="text-amber-600 font-bold hover:underline">로그인으로 돌아가기</router-link>
      <span class="text-stone-300">|</span>
      <router-link to="/find-id" class="text-amber-600 font-bold hover:underline">아이디 찾기</router-link>
    </div>
  </div>
</template>
