import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import MyPage from './MyPage.vue';
import { useAuthStore } from '@/stores/authStore';

const pushMock = vi.fn();

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { get: vi.fn(), put: vi.fn() },
  extractErrorMessage: (err: any, fallback: string) => err?.response?.data?.error || fallback,
}));

import apiClient from '@/api/apiClient';

function mountPage() {
  return mount(MyPage, {
    global: {
      stubs: { RouterLink: true, Teleport: true },
    },
  });
}

function loginAuthStore() {
  const authStore = useAuthStore();
  authStore.setAuth('token-abc', { id: 'u1', email: 'a@a.com', nickname: '닉네임', role: 'USER' });
  return authStore;
}

function makeSummary(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    userId: 'u1',
    email: 'a@a.com',
    nickname: '멍멍이집사',
    avatarUrl: null,
    role: 'USER',
    representativePet: {
      petId: 'pet-1',
      petName: '초코',
      breed: '골든리트리버',
      weightKg: 25.0,
      sizeCategory: 'LARGE',
    },
    petCount: 2,
    favoriteCount: 5,
    reviewCount: 3,
    ...overrides,
  };
}

function mockSummaryResponse(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    data: { success: true, data: makeSummary(overrides), error: null, timestamp: '2026-08-14T00:00:00' },
  };
}

describe('MyPage', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    pushMock.mockClear();
    (apiClient.get as any).mockReset();
    (apiClient.put as any).mockReset();
  });

  it('비로그인 사용자는 API를 호출하지 않고 로그인 화면으로 이동한다', async () => {
    mountPage();
    await flushPromises();

    expect(apiClient.get).not.toHaveBeenCalled();
    expect(pushMock).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/mypage' } });
  });

  it('로그인 사용자는 GET /users/me로 내 정보와 대표 반려동물을 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockSummaryResponse());

    const wrapper = mountPage();
    await flushPromises();

    expect(apiClient.get).toHaveBeenCalledWith('/users/me');
    expect(wrapper.text()).toContain('멍멍이집사');
    expect(wrapper.text()).toContain('a@a.com');
    expect(wrapper.text()).toContain('초코');
    expect(wrapper.text()).toContain('골든리트리버');
    expect(wrapper.text()).toContain('대형견');
    expect(wrapper.text()).toContain('2마리');
    expect(wrapper.text()).toContain('5개');
    expect(wrapper.text()).toContain('3개');
  });

  it('대표 반려동물이 없으면 안내 문구를 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockSummaryResponse({ representativePet: null }));

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('등록된 대표 반려동물이 없습니다.');
  });

  it('API 오류 시 기존 에러 처리 패턴으로 오류 메시지를 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockRejectedValue({
      response: { status: 500, data: { error: '서버 오류가 발생했습니다.' } },
    });

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('서버 오류가 발생했습니다.');
  });

  it('즐겨찾기 메뉴 클릭 시 즐겨찾기 화면으로 이동한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockSummaryResponse());

    const wrapper = mountPage();
    await flushPromises();

    const favoritesButton = wrapper.findAll('button').find((b) => b.text().includes('즐겨찾기'))!;
    await favoritesButton.trigger('click');

    expect(pushMock).toHaveBeenCalledWith({ name: 'Favorites' });
  });

  it('리뷰 메뉴 클릭 시 내 리뷰 화면으로 이동한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockSummaryResponse());

    const wrapper = mountPage();
    await flushPromises();

    const reviewButton = wrapper.findAll('button').find((b) => b.text().includes('작성 리뷰'))!;
    await reviewButton.trigger('click');

    expect(pushMock).toHaveBeenCalledWith({ name: 'MyReviews' });
  });

  it('반려동물 메뉴 클릭 시 내 반려동물 화면으로 이동한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockSummaryResponse());

    const wrapper = mountPage();
    await flushPromises();

    const petsButton = wrapper.findAll('button').find((b) => b.text().includes('마리'))!;
    await petsButton.trigger('click');

    expect(pushMock).toHaveBeenCalledWith({ name: 'Pets' });
  });

  it('내 정보 수정 성공 시 PUT /users/me 호출 후 정보가 갱신된다', async () => {
    loginAuthStore();
    let getCallCount = 0;
    (apiClient.get as any).mockImplementation(() => {
      getCallCount += 1;
      if (getCallCount === 1) return Promise.resolve(mockSummaryResponse());
      return Promise.resolve(mockSummaryResponse({ nickname: '새닉네임' }));
    });
    (apiClient.put as any).mockResolvedValue({
      data: {
        success: true,
        data: { id: 'u1', email: 'a@a.com', nickname: '새닉네임', avatarUrl: null, role: 'USER', provider: 'LOCAL', status: 'ACTIVE', createdAt: '2026-08-14T00:00:00' },
      },
    });

    const wrapper = mountPage();
    await flushPromises();

    const editButton = wrapper.findAll('button').find((b) => b.text() === '내 정보 수정')!;
    await editButton.trigger('click');
    await flushPromises();

    const nicknameInput = wrapper.find('input[placeholder="닉네임"]');
    await nicknameInput.setValue('새닉네임');

    const saveButton = wrapper.findAll('button').find((b) => b.text() === '저장')!;
    await saveButton.trigger('click');
    await flushPromises();

    expect(apiClient.put).toHaveBeenCalledWith('/users/me', { nickname: '새닉네임', avatarUrl: null });
    expect(wrapper.text()).toContain('새닉네임');
  });
});
