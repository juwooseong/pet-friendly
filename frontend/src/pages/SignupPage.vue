<script setup lang="ts">
import { ref, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import { isValidPassword, doPasswordsMatch } from '@/utils/validators';
import { debounce } from '@/utils/debounce';

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

// 실시간 중복확인 결과 (사용 가능 여부만 별도 상태로 보관 — 중복 시에는 기존 emailError/nicknameError를 재사용)
const emailAvailable = ref(false);
const nicknameAvailable = ref(false);

const emailInputRef = ref<InstanceType<typeof BaseInput> | null>(null);
const nicknameInputRef = ref<InstanceType<typeof BaseInput> | null>(null);
const passwordInputRef = ref<InstanceType<typeof BaseInput> | null>(null);
const passwordConfirmInputRef = ref<InstanceType<typeof BaseInput> | null>(null);

const EMAIL_FORMAT_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const NICKNAME_MIN_LENGTH = 2;
const NICKNAME_MAX_LENGTH = 50;

// 서버 응답이 요청 순서와 다르게 도착하는 경우(네트워크 지연 등) 최신 입력값 기준 응답만 반영하기 위한 토큰
let emailCheckToken = 0;
let nicknameCheckToken = 0;

const checkEmailAvailability = async (value: string) => {
  const token = ++emailCheckToken;
  try {
    const response = await apiClient.get('/auth/check-email', { params: { email: value } });
    if (token !== emailCheckToken) return; // 그 사이 값이 바뀌어 더 이상 최신 요청이 아님

    if (response.data?.success) {
      emailAvailable.value = response.data.data.available;
      emailError.value = response.data.data.available ? '' : '이미 사용 중인 이메일입니다.';
    }
  } catch {
    if (token !== emailCheckToken) return;
    // 실시간 확인 자체의 실패는 조용히 무시한다 — 최종 검증은 회원가입 제출 시 서버가 다시 수행한다.
    emailAvailable.value = false;
  }
};

const checkNicknameAvailability = async (value: string) => {
  const token = ++nicknameCheckToken;
  try {
    const response = await apiClient.get('/auth/check-nickname', { params: { nickname: value } });
    if (token !== nicknameCheckToken) return;

    if (response.data?.success) {
      nicknameAvailable.value = response.data.data.available;
      nicknameError.value = response.data.data.available ? '' : '이미 사용 중인 닉네임입니다.';
    }
  } catch {
    if (token !== nicknameCheckToken) return;
    nicknameAvailable.value = false;
  }
};

const debouncedCheckEmail = debounce(checkEmailAvailability, 400);
const debouncedCheckNickname = debounce(checkNicknameAvailability, 400);

const handleEmailUpdate = (value: string) => {
  email.value = value;
  emailError.value = '';
  emailAvailable.value = false;

  if (EMAIL_FORMAT_REGEX.test(value)) {
    debouncedCheckEmail(value);
  } else {
    debouncedCheckEmail.cancel();
  }
};

const handleNicknameUpdate = (value: string) => {
  nickname.value = value;
  nicknameError.value = '';
  nicknameAvailable.value = false;

  if (value.length >= NICKNAME_MIN_LENGTH && value.length <= NICKNAME_MAX_LENGTH) {
    debouncedCheckNickname(value);
  } else {
    debouncedCheckNickname.cancel();
  }
};

onUnmounted(() => {
  debouncedCheckEmail.cancel();
  debouncedCheckNickname.cancel();
});

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
    const message = extractErrorMessage(err, '회원가입 처리 중 오류가 발생했습니다.');

    if (message.includes('닉네임')) {
      nicknameError.value = message;
      nicknameInputRef.value?.focus();
    } else if (message.includes('이메일')) {
      emailError.value = message;
      emailInputRef.value?.focus();
    } else {
      errorMessage.value = message;
    }
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
      <div class="flex flex-col gap-1">
        <BaseInput
          ref="emailInputRef"
          :model-value="email"
          label="이메일 주소"
          placeholder="example@petspot.com"
          type="email"
          :error="emailError"
          @update:modelValue="handleEmailUpdate"
        />
        <p v-if="emailAvailable && !emailError" class="text-xs text-emerald-600 px-1">
          사용 가능한 이메일입니다.
        </p>
      </div>

      <div class="flex flex-col gap-1">
        <BaseInput
          ref="nicknameInputRef"
          :model-value="nickname"
          label="닉네임"
          placeholder="뽀삐아빠"
          :error="nicknameError"
          @update:modelValue="handleNicknameUpdate"
        />
        <p v-if="nicknameAvailable && !nicknameError" class="text-xs text-emerald-600 px-1">
          사용 가능한 닉네임입니다.
        </p>
      </div>

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
