<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import apiClient, { extractErrorMessage } from '@/api/apiClient';

const router = useRouter();

const email = ref('');
const password = ref('');
const nickname = ref('');
const loading = ref(false);
const errorMessage = ref('');

const handleSignup = async () => {
  if (!email.value || !password.value || !nickname.value) {
    errorMessage.value = '모든 필드를 입력해주세요.';
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const response = await apiClient.post('/auth/register', {
      email: email.value,
      password: password.value,
      nickname: nickname.value,
    });

    if (response.data && response.data.success) {
      alert('회원가입이 완료되었습니다! 로그인해주세요.');
      router.push('/login');
    } else {
      errorMessage.value = response.data.error || '회원가입 실패';
    }
  } catch (err: any) {
    errorMessage.value = extractErrorMessage(err, '회원가입 처리 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="space-y-6">
    <div class="text-center">
      <h2 class="text-2xl font-bold text-stone-800">PetSpot 회원가입 🐶</h2>
      <p class="text-xs text-stone-500 mt-1">반려동물과 함께할 안심 장소를 찾아보세요.</p>
    </div>

    <ErrorMessage v-if="errorMessage" :message="errorMessage" />

    <form @submit.prevent="handleSignup" class="space-y-4">
      <BaseInput
        v-model="email"
        label="이메일 주소"
        placeholder="example@petspot.com"
        type="email"
      />

      <BaseInput
        v-model="nickname"
        label="닉네임"
        placeholder="뽀삐아빠"
      />

      <BaseInput
        v-model="password"
        label="비밀번호"
        placeholder="8자 이상 영문/숫자/특수문자"
        type="password"
      />

      <BaseButton type="submit" variant="primary" size="lg" :loading="loading" class="w-full mt-2">
        회원가입 신청
      </BaseButton>
    </form>

    <div class="text-center text-xs text-stone-500 pt-2 border-t border-stone-100">
      이미 계정이 있으신가요?
      <router-link to="/login" class="text-amber-600 font-bold hover:underline">로그인하기</router-link>
    </div>
  </div>
</template>
