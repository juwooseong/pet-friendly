<script setup lang="ts">
import { ref } from 'vue';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import apiClient, { extractErrorMessage } from '@/api/apiClient';

const nickname = ref('');
const loading = ref(false);
const errorMessage = ref('');
const maskedEmail = ref('');

const handleFindId = async () => {
  if (!nickname.value) {
    errorMessage.value = '닉네임을 입력해주세요.';
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  maskedEmail.value = '';

  try {
    const response = await apiClient.post('/auth/find-id', {
      nickname: nickname.value,
    });

    if (response.data && response.data.success) {
      maskedEmail.value = response.data.data.maskedEmail;
    } else {
      errorMessage.value = response.data.error || '아이디 찾기에 실패했습니다.';
    }
  } catch (err: any) {
    errorMessage.value = extractErrorMessage(err, '일치하는 회원 정보를 찾을 수 없습니다.');
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="space-y-6">
    <div class="text-center">
      <h2 class="text-2xl font-bold text-stone-800">아이디 찾기 🔍</h2>
      <p class="text-xs text-stone-500 mt-1">가입 시 등록한 닉네임으로 이메일을 확인해드려요.</p>
    </div>

    <ErrorMessage v-if="errorMessage" :message="errorMessage" />

    <div v-if="maskedEmail" class="bg-amber-50 border border-amber-200 rounded-xl p-4 text-center space-y-1">
      <p class="text-xs text-stone-500">회원님의 이메일 주소예요</p>
      <p class="text-lg font-bold text-amber-700">{{ maskedEmail }}</p>
    </div>

    <form v-else @submit.prevent="handleFindId" class="space-y-4">
      <BaseInput
        v-model="nickname"
        label="닉네임"
        placeholder="가입 시 등록한 닉네임"
      />

      <BaseButton type="submit" variant="primary" size="lg" :loading="loading" class="w-full mt-2">
        아이디 찾기
      </BaseButton>
    </form>

    <div class="text-center text-xs text-stone-500 pt-2 border-t border-stone-100 flex justify-center gap-4">
      <router-link to="/login" class="text-amber-600 font-bold hover:underline">로그인으로 돌아가기</router-link>
      <span class="text-stone-300">|</span>
      <router-link to="/find-password" class="text-amber-600 font-bold hover:underline">비밀번호 찾기</router-link>
    </div>
  </div>
</template>
