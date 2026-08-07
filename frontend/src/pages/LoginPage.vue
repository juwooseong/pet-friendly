<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import apiClient from '@/api/apiClient';

const router = useRouter();
const authStore = useAuthStore();

const email = ref('');
const password = ref('');
const loading = ref(false);
const errorMessage = ref('');

const handleLogin = async () => {
  if (!email.value || !password.value) {
    errorMessage.value = '이메일과 비밀번호를 입력해주세요.';
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const response = await apiClient.post('/auth/login', {
      email: email.value,
      password: password.value,
    });

    if (response.data && response.data.success) {
      const { accessToken, user } = response.data.data;
      authStore.setAuth(accessToken, user);
      router.push('/');
    } else {
      errorMessage.value = response.data.message || '로그인에 실패했습니다.';
    }
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message || '로그인 처리 중 오류가 발생했습니다.';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="space-y-6">
    <div class="text-center">
      <h2 class="text-2xl font-bold text-stone-800">반가워요! 🐾</h2>
      <p class="text-xs text-stone-500 mt-1">PetSpot 계정으로 로그인해주세요.</p>
    </div>

    <ErrorMessage v-if="errorMessage" :message="errorMessage" />

    <form @submit.prevent="handleLogin" class="space-y-4">
      <BaseInput
        v-model="email"
        label="이메일 주소"
        placeholder="example@petspot.com"
        type="email"
      />

      <BaseInput
        v-model="password"
        label="비밀번호"
        placeholder="••••••••"
        type="password"
      />

      <BaseButton type="submit" variant="primary" size="lg" :loading="loading" class="w-full mt-2">
        로그인
      </BaseButton>
    </form>

    <div class="text-center text-xs text-stone-500 pt-2 border-t border-stone-100">
      계정이 없으신가요?
      <router-link to="/signup" class="text-amber-600 font-bold hover:underline">회원가입하기</router-link>
    </div>
  </div>
</template>
