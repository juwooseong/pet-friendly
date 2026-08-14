import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import SignupPage from './SignupPage.vue';

const pushMock = vi.fn();

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  extractErrorMessage: (err: any, fallback: string) => err?.response?.data?.error || fallback,
}));

import apiClient from '@/api/apiClient';

window.alert = vi.fn();

function mountPage() {
  return mount(SignupPage, {
    global: {
      stubs: { RouterLink: true },
    },
  });
}

function nicknameInputOf(wrapper: ReturnType<typeof mountPage>) {
  return wrapper.find('input:not([type="email"]):not([type="password"])');
}

async function advanceDebounce() {
  vi.advanceTimersByTime(400);
  await flushPromises();
}

describe('SignupPage - 실시간 이메일/닉네임 중복확인', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    pushMock.mockClear();
    (apiClient.get as any).mockReset();
    (apiClient.post as any).mockReset();
    (window.alert as any).mockClear();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('정상 형식 이메일 입력 시 400ms 디바운스 후 check-email API를 호출한다', async () => {
    (apiClient.get as any).mockResolvedValue({ data: { success: true, data: { available: true } } });
    const wrapper = mountPage();

    await wrapper.find('input[type="email"]').setValue('user@petspot.com');
    expect(apiClient.get).not.toHaveBeenCalled();

    await advanceDebounce();

    expect(apiClient.get).toHaveBeenCalledWith('/auth/check-email', { params: { email: 'user@petspot.com' } });
  });

  it('이메일 형식이 잘못되면 디바운스 이후에도 API를 호출하지 않는다', async () => {
    const wrapper = mountPage();

    await wrapper.find('input[type="email"]').setValue('not-an-email');
    await advanceDebounce();

    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('이메일 사용 가능 응답 시 사용 가능 메시지를 표시한다', async () => {
    (apiClient.get as any).mockResolvedValue({ data: { success: true, data: { available: true } } });
    const wrapper = mountPage();

    await wrapper.find('input[type="email"]').setValue('user@petspot.com');
    await advanceDebounce();

    expect(wrapper.text()).toContain('사용 가능한 이메일입니다.');
  });

  it('이메일 중복 응답 시 중복 메시지를 표시한다', async () => {
    (apiClient.get as any).mockResolvedValue({ data: { success: true, data: { available: false } } });
    const wrapper = mountPage();

    await wrapper.find('input[type="email"]').setValue('taken@petspot.com');
    await advanceDebounce();

    expect(wrapper.text()).toContain('이미 사용 중인 이메일입니다.');
  });

  it('정상 닉네임 입력 시 400ms 디바운스 후 check-nickname API를 호출한다', async () => {
    (apiClient.get as any).mockResolvedValue({ data: { success: true, data: { available: true } } });
    const wrapper = mountPage();

    await nicknameInputOf(wrapper).setValue('뽀삐아빠');
    expect(apiClient.get).not.toHaveBeenCalled();

    await advanceDebounce();

    expect(apiClient.get).toHaveBeenCalledWith('/auth/check-nickname', { params: { nickname: '뽀삐아빠' } });
  });

  it('1자 닉네임(길이 정책 위반) 입력 시 API를 호출하지 않는다', async () => {
    const wrapper = mountPage();

    await nicknameInputOf(wrapper).setValue('a');
    await advanceDebounce();

    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('닉네임 중복 응답 시 중복 메시지를 표시한다', async () => {
    (apiClient.get as any).mockResolvedValue({ data: { success: true, data: { available: false } } });
    const wrapper = mountPage();

    await nicknameInputOf(wrapper).setValue('중복닉네임');
    await advanceDebounce();

    expect(wrapper.text()).toContain('이미 사용 중인 닉네임입니다.');
  });

  it('빠른 연속 입력 시 마지막 입력값 기준으로만 API를 1회 호출한다', async () => {
    (apiClient.get as any).mockResolvedValue({ data: { success: true, data: { available: true } } });
    const wrapper = mountPage();
    const emailInput = wrapper.find('input[type="email"]');

    await emailInput.setValue('a@petspot.com');
    vi.advanceTimersByTime(200);
    await emailInput.setValue('ab@petspot.com');
    vi.advanceTimersByTime(200);
    await emailInput.setValue('abc@petspot.com');
    await advanceDebounce();

    expect(apiClient.get).toHaveBeenCalledTimes(1);
    expect(apiClient.get).toHaveBeenCalledWith('/auth/check-email', { params: { email: 'abc@petspot.com' } });
  });

  it('실시간 체크가 available=true여도 최종 register 409 응답을 정상적으로 처리한다 (Race Condition 방어)', async () => {
    (apiClient.get as any).mockResolvedValue({ data: { success: true, data: { available: true } } });
    (apiClient.post as any).mockRejectedValue({
      response: { data: { error: '이미 사용 중인 이메일입니다.' } },
    });

    const wrapper = mountPage();

    await wrapper.find('input[type="email"]').setValue('race@petspot.com');
    await advanceDebounce();
    expect(wrapper.text()).toContain('사용 가능한 이메일입니다.');

    await nicknameInputOf(wrapper).setValue('레이스테스트');
    await advanceDebounce();

    const passwordInputs = wrapper.findAll('input[type="password"]');
    await passwordInputs[0].setValue('Password1!');
    await passwordInputs[1].setValue('Password1!');

    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    expect(apiClient.post).toHaveBeenCalledWith('/auth/register', expect.objectContaining({ email: 'race@petspot.com' }));
    expect(wrapper.text()).toContain('이미 사용 중인 이메일입니다.');
    expect(pushMock).not.toHaveBeenCalled();
  });
});
