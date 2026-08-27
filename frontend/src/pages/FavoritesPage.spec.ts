import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import FavoritesPage from './FavoritesPage.vue';
import { useAuthStore } from '@/stores/authStore';

const pushMock = vi.fn();

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { get: vi.fn(), delete: vi.fn() },
  extractErrorMessage: (err: any, fallback: string) => err?.response?.data?.error || fallback,
}));

import apiClient from '@/api/apiClient';

function mountPage() {
  return mount(FavoritesPage, {
    global: {
      stubs: { RouterLink: true },
    },
  });
}

function loginAuthStore() {
  const authStore = useAuthStore();
  authStore.setAuth('token-abc', { id: 'u1', email: 'a@a.com', nickname: '닉네임', pets: [] });
  return authStore;
}

function makeFavorite(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    favoriteId: 'fav-1',
    placeId: 'place-123',
    placeName: '홍대 애견카페',
    category: 'CAFE',
    categoryName: '카페',
    address: '서울특별시 마포구',
    imageUrl: null,
    rating: 4.5,
    reviewCount: 10,
    createdAt: '2026-08-14T00:00:00',
    ...overrides,
  };
}

function mockFavoritesResponse(favorites: Array<Record<string, unknown>> = []) {
  return {
    data: {
      success: true,
      data: favorites,
      error: null,
      timestamp: '2026-08-14T00:00:00',
    },
  };
}

describe('FavoritesPage', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    pushMock.mockClear();
    (apiClient.get as any).mockReset();
    (apiClient.delete as any).mockReset();
  });

  it('비로그인 사용자는 API를 호출하지 않고 로그인 화면으로 이동한다', async () => {
    mountPage();
    await flushPromises();

    expect(apiClient.get).not.toHaveBeenCalled();
    expect(pushMock).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/favorites' } });
  });

  it('로그인 사용자는 GET /users/me/favorites로 즐겨찾기 목록을 조회해 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockFavoritesResponse([makeFavorite()]));

    const wrapper = mountPage();
    await flushPromises();

    expect(apiClient.get).toHaveBeenCalledWith('/users/me/favorites');
    expect(wrapper.text()).toContain('홍대 애견카페');
    expect(wrapper.text()).toContain('서울특별시 마포구');
    expect(wrapper.text()).toContain('카페');
    expect(wrapper.text()).toContain('리뷰 10개');
  });

  it('로딩 중에는 로딩 스피너를 표시한다', async () => {
    loginAuthStore();
    let resolvePromise: (value: any) => void = () => {};
    (apiClient.get as any).mockReturnValue(
      new Promise((resolve) => {
        resolvePromise = resolve;
      })
    );

    const wrapper = mountPage();
    expect(wrapper.text()).toContain('즐겨찾기 목록 불러오는 중');

    resolvePromise(mockFavoritesResponse([]));
    await flushPromises();
    expect(wrapper.text()).not.toContain('즐겨찾기 목록 불러오는 중');
  });

  it('즐겨찾기가 없으면 EmptyState 형태의 안내를 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockFavoritesResponse([]));

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('등록된 즐겨찾기가 없습니다.');
  });

  it('API 오류 시 기존 에러 처리 패턴(ErrorMessage)으로 오류 메시지를 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockRejectedValue({
      response: { status: 500, data: { error: '서버 오류가 발생했습니다.' } },
    });

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('서버 오류가 발생했습니다.');
  });

  it('카드를 클릭하면 장소 상세 페이지로 이동한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockFavoritesResponse([makeFavorite()]));

    const wrapper = mountPage();
    await flushPromises();

    await wrapper.find('.rounded-2xl.p-5').trigger('click');

    expect(pushMock).toHaveBeenCalledWith({ name: 'PlaceDetail', params: { id: 'place-123' } });
  });

  it('삭제 버튼 클릭 시 DELETE /favorites/{placeId} 호출 후 목록에서 즉시 제거된다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockFavoritesResponse([makeFavorite()]));
    (apiClient.delete as any).mockResolvedValue({ data: { success: true, data: null } });

    const wrapper = mountPage();
    await flushPromises();

    const deleteButton = wrapper.findAll('button').find((b) => b.text().includes('삭제'))!;
    await deleteButton.trigger('click');
    await flushPromises();

    expect(apiClient.delete).toHaveBeenCalledWith('/favorites/place-123');
    expect(wrapper.text()).not.toContain('홍대 애견카페');
    expect(pushMock).not.toHaveBeenCalledWith(
      expect.objectContaining({ name: 'PlaceDetail' })
    );
  });

  it('삭제 API 실패 시 목록을 유지하고 오류 메시지를 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockFavoritesResponse([makeFavorite()]));
    (apiClient.delete as any).mockRejectedValue({
      response: { status: 500, data: { error: '즐겨찾기 삭제 실패' } },
    });

    const wrapper = mountPage();
    await flushPromises();

    const deleteButton = wrapper.findAll('button').find((b) => b.text().includes('삭제'))!;
    await deleteButton.trigger('click');
    await flushPromises();

    expect(wrapper.text()).toContain('홍대 애견카페');
    expect(wrapper.text()).toContain('즐겨찾기 삭제 실패');
  });

  it('삭제 요청이 진행 중일 때는 버튼이 비활성화되어 중복 요청이 발생하지 않는다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockFavoritesResponse([makeFavorite()]));
    let resolveDelete: (value: any) => void = () => {};
    (apiClient.delete as any).mockReturnValue(
      new Promise((resolve) => {
        resolveDelete = resolve;
      })
    );

    const wrapper = mountPage();
    await flushPromises();

    const deleteButton = wrapper.findAll('button').find((b) => b.text().includes('삭제'))!;
    await deleteButton.trigger('click');
    await deleteButton.trigger('click');
    await deleteButton.trigger('click');

    expect(apiClient.delete).toHaveBeenCalledTimes(1);
    expect(wrapper.findAll('button').find((b) => b.text().includes('삭제'))!.attributes('disabled')).toBeDefined();

    resolveDelete({ data: { success: true, data: null } });
    await flushPromises();
  });
});
