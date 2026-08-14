import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';
import { useAuthStore } from '@/stores/authStore';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

// 백엔드가 강제 비밀번호 변경으로 인한 403에만 붙이는 식별 헤더 (다른 403과 구분하기 위함)
const PASSWORD_CHANGE_REQUIRED_HEADER = 'x-password-change-required';

export const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Inject JWT Bearer Token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('accessToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor: Handle API responses & 401/403 에러 분리 처리
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      // 401: 인증 만료/실패 - 기존과 동일하게 토큰을 지우고 로그인 화면으로 이동
      console.warn('[Axios Interceptor] 401 Unauthorized - Clearing token');
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
      localStorage.removeItem('requiresPasswordChange');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    } else if (status === 403 && error.response?.headers?.[PASSWORD_CHANGE_REQUIRED_HEADER] === 'true') {
      // 403(강제 비밀번호 변경 필요): 로그아웃시키지 않고 authStore 상태만 동기화한다.
      // App.vue에 전역 마운트된 ForcePasswordChangeModal이 이 상태를 보고 자동으로 표시된다.
      console.warn('[Axios Interceptor] 403 Password Change Required');
      const authStore = useAuthStore();
      authStore.flagPasswordChangeRequired();
    }

    return Promise.reject(error);
  }
);

/**
 * 백엔드 ApiResponse({ success, data, error, timestamp })에서 비즈니스 오류 메시지를 추출한다.
 * 서버가 내려준 메시지를 우선 사용하고, 없을 때만 fallback 문구를 사용한다.
 */
export const extractErrorMessage = (err: any, fallback: string): string => {
  const body = err?.response?.data;
  if (typeof body?.error === 'string' && body.error.trim()) {
    return body.error;
  }
  return fallback;
};

export default apiClient;
