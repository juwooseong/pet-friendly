import { describe, it, expect, beforeEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import apiClient from './apiClient';
import { useAuthStore } from '@/stores/authStore';

function getRejectedHandler(): (error: any) => Promise<never> {
  const handlers = (apiClient.interceptors.response as any).handlers;
  return handlers[handlers.length - 1].rejected;
}

describe('apiClient response interceptor - 401/403 분리 처리', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
  });

  it('401 응답 시 토큰/사용자/강제변경 상태를 모두 정리한다 (기존 로그아웃 처리 유지)', async () => {
    const authStore = useAuthStore();
    authStore.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'n' } as any, true);

    const rejected = getRejectedHandler();
    const error = { response: { status: 401, headers: {} } };

    await expect(rejected(error)).rejects.toBe(error);

    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    expect(localStorage.getItem('requiresPasswordChange')).toBeNull();
  });

  it('강제 비밀번호 변경 헤더가 포함된 403 응답 시 로그아웃 대신 requiresPasswordChange를 true로 설정한다', async () => {
    const authStore = useAuthStore();
    authStore.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'n' } as any, false);

    const rejected = getRejectedHandler();
    const error = {
      response: { status: 403, headers: { 'x-password-change-required': 'true' } },
    };

    await expect(rejected(error)).rejects.toBe(error);

    expect(authStore.requiresPasswordChange).toBe(true);
    // 401과 달리 로그아웃되지 않아야 하므로 토큰은 유지되어야 한다
    expect(localStorage.getItem('accessToken')).toBe('token-abc');
  });

  it('강제 비밀번호 변경과 무관한 403 응답(예: 권한 없음)은 requiresPasswordChange를 건드리지 않는다', async () => {
    const authStore = useAuthStore();
    authStore.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'n' } as any, false);

    const rejected = getRejectedHandler();
    const error = { response: { status: 403, headers: {} } };

    await expect(rejected(error)).rejects.toBe(error);

    expect(authStore.requiresPasswordChange).toBe(false);
    expect(localStorage.getItem('accessToken')).toBe('token-abc');
  });
});
