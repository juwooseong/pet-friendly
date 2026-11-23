import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises, RouterLinkStub } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import ResetPasswordPage from './ResetPasswordPage.vue';
import { useAuthStore } from '@/stores/authStore';

const replaceMock = vi.fn();
let query: Record<string, string> = {};

vi.mock('vue-router', () => ({
  useRoute: () => ({ path: '/reset-password', query }),
  useRouter: () => ({ replace: replaceMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { post: vi.fn() },
  extractErrorMessage: (err: any, fallback: string) => err?.response?.data?.error ?? fallback,
}));

import apiClient from '@/api/apiClient';

function mountPage() {
  return mount(ResetPasswordPage, { global: { stubs: { RouterLink: RouterLinkStub } } });
}

async function submit(wrapper: ReturnType<typeof mountPage>) {
  await flushPromises(); // onMounted에서 토큰을 읽은 뒤 폼이 그려진다
  const inputs = wrapper.findAll('input[type="password"]');
  await inputs[0].setValue('NewPassw0rd!');
  await inputs[1].setValue('NewPassw0rd!');
  await wrapper.find('form').trigger('submit.prevent');
  await flushPromises();
}

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    replaceMock.mockClear();
    (apiClient.post as any).mockReset();
    query = {};
  });

  it('링크에 토큰이 없으면 입력 폼 대신 다시 요청하라는 안내를 보여준다', () => {
    const wrapper = mountPage();

    expect(wrapper.find('form').exists()).toBe(false);
    expect(wrapper.text()).toContain('재설정 링크가 올바르지 않습니다');
  });

  it('토큰으로 재설정에 성공하면 주소에서 토큰을 지우고, 완료 안내와 함께 이 브라우저의 로그인을 정리한다', async () => {
    query = { token: 'raw-token' };
    const authStore = useAuthStore();
    authStore.setAuth('old-token', { id: '1', email: 'a@a.com', nickname: 'nick', role: 'USER' });

    const wrapper = mountPage();
    expect(replaceMock).toHaveBeenCalledWith({ path: '/reset-password', query: {} });

    (apiClient.post as any).mockResolvedValue({ data: { success: true, data: null, error: null } });
    await submit(wrapper);

    expect(apiClient.post).toHaveBeenCalledWith('/auth/reset-password', {
      token: 'raw-token',
      newPassword: 'NewPassw0rd!',
      confirmPassword: 'NewPassw0rd!',
    });
    expect(wrapper.text()).toContain('비밀번호가 변경되었습니다');
    expect(authStore.isAuthenticated).toBe(false);
  });

  it('만료·사용된 링크면 서버 오류 문구를 보여주고 폼을 유지한다', async () => {
    query = { token: 'used-token' };
    (apiClient.post as any).mockRejectedValue({
      response: { data: { error: '비밀번호 재설정 링크가 만료되었거나 이미 사용되었습니다. 비밀번호 찾기를 다시 요청해주세요.' } },
    });

    const wrapper = mountPage();
    await submit(wrapper);

    expect(wrapper.text()).toContain('만료되었거나 이미 사용되었습니다');
    expect(wrapper.find('form').exists()).toBe(true);
  });
});
