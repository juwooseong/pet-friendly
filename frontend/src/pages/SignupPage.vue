<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import { isValidPassword, doPasswordsMatch } from '@/utils/validators';

const router = useRouter();

const email = ref('');
const password = ref('');
const passwordConfirm = ref('');
const nickname = ref('');
const loading = ref(false);
const errorMessage = ref('');
const emailError = ref('');
const nicknameError = ref('');
const passwordError = ref('');
const passwordConfirmError = ref('');

const emailInputRef = ref<InstanceType<typeof BaseInput> | null>(null);
const nicknameInputRef = ref<InstanceType<typeof BaseInput> | null>(null);
const passwordInputRef = ref<InstanceType<typeof BaseInput> | null>(null);
const passwordConfirmInputRef = ref<InstanceType<typeof BaseInput> | null>(null);

const validateRequiredFields = (): boolean => {
  if (!email.value) {
    emailError.value = '이메일을 입력해주세요.';
    emailInputRef.value?.focus();
    return false;
  }
  if (!nickname.value) {
    nicknameError.value = '닉네임을 입력해주세요.';
    nicknameInputRef.value?.focus();
    return false;
  }
  if (!password.value) {
    passwordError.value = '비밀번호를 입력해주세요.';
    passwordInputRef.value?.focus();
    return false;
  }
  if (!passwordConfirm.value) {
    passwordConfirmError.value = '비밀번호 확인을 입력해주세요.';
    passwordConfirmInputRef.value?.focus();
    return false;
  }
  return true;
};

const handleSignup = async () => {
  emailError.value = '';
  nicknameError.value = '';
  passwordError.value = '';
  passwordConfirmError.value = '';
  errorMessage.value = '';

  if (!validateRequiredFields()) {
    return;
  }

  if (!isValidPassword(password.value)) {
    passwordError.value = '비밀번호는 8자 이상, 영문/숫자/특수문자를 모두 포함해야 합니다.';
    passwordInputRef.value?.focus();
    return;
  }

  if (!doPasswordsMatch(password.value, passwordConfirm.value)) {
    passwordConfirmError.value = '비밀번호가 일치하지 않습니다.';
    passwordConfirmInputRef.value?.focus();
    return;
  }

  loading.value = true;

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
        ref="emailInputRef"
        v-model="email"
        label="이메일 주소"
        placeholder="example@petspot.com"
        type="email"
        :error="emailError"
        @update:modelValue="emailError = ''"
      />

      <BaseInput
        ref="nicknameInputRef"
        v-model="nickname"
        label="닉네임"
        placeholder="뽀삐아빠"
        :error="nicknameError"
        @update:modelValue="nicknameError = ''"
      />

      <BaseInput
        ref="passwordInputRef"
        v-model="password"
        label="비밀번호"
        placeholder="8자 이상 영문/숫자/특수문자"
        type="password"
        :error="passwordError"
        @update:modelValue="passwordError = ''"
      />

      <BaseInput
        ref="passwordConfirmInputRef"
        v-model="passwordConfirm"
        label="비밀번호 확인"
        placeholder="비밀번호를 다시 입력해주세요"
        type="password"
        :error="passwordConfirmError"
        @update:modelValue="passwordConfirmError = ''"
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
