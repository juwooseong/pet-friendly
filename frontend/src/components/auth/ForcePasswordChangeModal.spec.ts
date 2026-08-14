import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import ForcePasswordChangeModal from './ForcePasswordChangeModal.vue';
import { useAuthStore } from '@/stores/authStore';

const pushMock = vi.fn();

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { patch: vi.fn() },
  extractErrorMessage: (_err: any, fallback: string) => fallback,
}));

import apiClient from '@/api/apiClient';

window.alert = vi.fn();

function mountModal() {
  return mount(ForcePasswordChangeModal);
}

describe('ForcePasswordChangeModal', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    pushMock.mockClear();
    (apiClient.patch as any).mockReset();
    (window.alert as any).mockClear();
  });

  it('닫기/취소 버튼을 제공하지 않는다 (우회 불가)', () => {
    const wrapper = mountModal();
    const buttonTexts = wrapper.findAll('button').map((b) => b.text());

    expect(buttonTexts.some((text) => text.includes('취소'))).toBe(false);
    expect(buttonTexts.some((text) => text.includes('닫기'))).toBe(false);
    expect(wrapper.find('button[aria-label="close"]').exists()).toBe(false);
  });

  it('비밀번호 정책(영문/숫자/특수문자 8자 이상)에 맞지 않으면 API를 호출하지 않는다', async () => {
    const wrapper = mountModal();
    const inputs = wrapper.findAll('input[type="password"]');

    await inputs[0].setValue('onlyletters');
    await inputs[1].setValue('onlyletters');
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    expect(apiClient.patch).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('영문/숫자/특수문자');
  });

  it('새 비밀번호와 확인 비밀번호가 일치하지 않으면 API를 호출하지 않는다', async () => {
    const wrapper = mountModal();
    const inputs = wrapper.findAll('input[type="password"]');

    await inputs[0].setValue('NewPassw0rd!');
    await inputs[1].setValue('Different1!');
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    expect(apiClient.patch).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('일치하지 않습니다');
  });

  it('정상 비밀번호 입력 시 PATCH /auth/password 호출 후 requiresPasswordChange=false로 바뀌고 홈으로 이동한다', async () => {
    (apiClient.patch as any).mockResolvedValue({
      data: { success: true, data: null, error: null },
    });

    const authStore = useAuthStore();
    authStore.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'nick' } as any, true);

    const wrapper = mountModal();
    const inputs = wrapper.findAll('input[type="password"]');

    await inputs[0].setValue('NewPassw0rd!');
    await inputs[1].setValue('NewPassw0rd!');
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    expect(apiClient.patch).toHaveBeenCalledWith('/auth/password', {
      newPassword: 'NewPassw0rd!',
      confirmPassword: 'NewPassw0rd!',
    });
    expect(authStore.requiresPasswordChange).toBe(false);
    expect(pushMock).toHaveBeenCalledWith('/');
  });

  it('API 실패 시 에러 메시지를 표시하고 requiresPasswordChange는 그대로 유지한다', async () => {
    (apiClient.patch as any).mockRejectedValue({
      response: { data: { error: '비밀번호 변경 실패' } },
    });

    const authStore = useAuthStore();
    authStore.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'nick' } as any, true);

    const wrapper = mountModal();
    const inputs = wrapper.findAll('input[type="password"]');

    await inputs[0].setValue('NewPassw0rd!');
    await inputs[1].setValue('NewPassw0rd!');
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    expect(authStore.requiresPasswordChange).toBe(true);
    expect(pushMock).not.toHaveBeenCalled();
  });
});
