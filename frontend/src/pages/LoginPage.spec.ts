import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import LoginPage from './LoginPage.vue';
import { useAuthStore } from '@/stores/authStore';

const pushMock = vi.fn();

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { post: vi.fn() },
  extractErrorMessage: (_err: any, fallback: string) => fallback,
}));

import apiClient from '@/api/apiClient';

function mountPage() {
  return mount(LoginPage, {
    global: {
      stubs: { RouterLink: true },
    },
  });
}

describe('LoginPage - requiresPasswordChange 처리', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    pushMock.mockClear();
    (apiClient.post as any).mockReset();
  });

  it('일반 로그인 성공(requiresPasswordChange=false) 시 홈으로 이동한다', async () => {
    (apiClient.post as any).mockResolvedValue({
      data: {
        success: true,
        data: {
          accessToken: 'token-123',
          tokenType: 'Bearer',
          expiresIn: 86400,
          requiresPasswordChange: false,
          user: { id: '1', email: 'a@a.com', nickname: '닉네임' },
        },
      },
    });

    const wrapper = mountPage();
    await wrapper.find('input[type="email"]').setValue('a@a.com');
    await wrapper.find('input[type="password"]').setValue('Password1!');
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    const authStore = useAuthStore();
    expect(authStore.requiresPasswordChange).toBe(false);
    expect(authStore.isAuthenticated).toBe(true);
    expect(pushMock).toHaveBeenCalledWith('/');
  });

  it('임시 비밀번호 로그인(requiresPasswordChange=true) 시 홈으로 이동하지 않고 상태만 저장한다', async () => {
    (apiClient.post as any).mockResolvedValue({
      data: {
        success: true,
        data: {
          accessToken: 'token-456',
          tokenType: 'Bearer',
          expiresIn: 86400,
          requiresPasswordChange: true,
          user: { id: '1', email: 'a@a.com', nickname: '닉네임' },
        },
      },
    });

    const wrapper = mountPage();
    await wrapper.find('input[type="email"]').setValue('a@a.com');
    await wrapper.find('input[type="password"]').setValue('TempPassw0rd!');
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    const authStore = useAuthStore();
    expect(authStore.requiresPasswordChange).toBe(true);
    expect(authStore.isAuthenticated).toBe(true);
    expect(pushMock).not.toHaveBeenCalled();
  });
});
