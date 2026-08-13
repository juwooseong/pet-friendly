import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

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

// Response Interceptor: Handle API responses & 401 Unauthorized
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      console.warn('[Axios Interceptor] 401 Unauthorized - Clearing token');
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
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
