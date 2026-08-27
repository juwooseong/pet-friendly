import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import MyReviewsPage from './MyReviewsPage.vue';
import { useAuthStore } from '@/stores/authStore';

const pushMock = vi.fn();

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { get: vi.fn() },
  extractErrorMessage: (err: any, fallback: string) => err?.response?.data?.error || fallback,
}));

import apiClient from '@/api/apiClient';

function mountPage() {
  return mount(MyReviewsPage, {
    global: { stubs: { RouterLink: true } },
  });
}

function loginAuthStore() {
  const authStore = useAuthStore();
  authStore.setAuth('token-abc', { id: 'u1', email: 'a@a.com', nickname: '닉네임', pets: [] });
  return authStore;
}

function makeReview(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 'review-1',
    placeId: 'place-123',
    placeName: '홍대 애견카페',
    userId: 'u1',
    userNickname: '닉네임',
    userAvatarUrl: null,
    rating: 5,
    content: '너무 좋았어요!',
    imageUrl: null,
    createdAt: '2026-08-14T00:00:00Z',
    ...overrides,
  };
}

function mockReviewsResponse(reviews: Array<Record<string, unknown>> = []) {
  return { data: { success: true, data: reviews, error: null, timestamp: '2026-08-14T00:00:00' } };
}

describe('MyReviewsPage', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    pushMock.mockClear();
    (apiClient.get as any).mockReset();
  });

  it('비로그인 사용자는 API를 호출하지 않고 로그인 화면으로 이동한다', async () => {
    mountPage();
    await flushPromises();

    expect(apiClient.get).not.toHaveBeenCalled();
    expect(pushMock).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/mypage/reviews' } });
  });

  it('로그인 사용자는 GET /users/me/reviews로 내 리뷰 목록을 조회해 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockReviewsResponse([makeReview()]));

    const wrapper = mountPage();
    await flushPromises();

    expect(apiClient.get).toHaveBeenCalledWith('/users/me/reviews');
    expect(wrapper.text()).toContain('홍대 애견카페');
    expect(wrapper.text()).toContain('너무 좋았어요!');
  });

  it('리뷰가 없으면 EmptyState 형태의 안내를 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockReviewsResponse([]));

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('아직 작성한 리뷰가 없습니다.');
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

  it('리뷰 카드를 클릭하면 장소 상세 페이지로 이동한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockReviewsResponse([makeReview()]));

    const wrapper = mountPage();
    await flushPromises();

    await wrapper.find('.rounded-2xl.p-5').trigger('click');

    expect(pushMock).toHaveBeenCalledWith({ name: 'PlaceDetail', params: { id: 'place-123' } });
  });
});
