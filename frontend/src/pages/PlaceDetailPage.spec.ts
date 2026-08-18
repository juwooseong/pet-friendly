import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import PlaceDetailPage from './PlaceDetailPage.vue';
import { useAuthStore } from '@/stores/authStore';

const pushMock = vi.fn();

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: 'place-123' }, fullPath: '/place/place-123' }),
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), delete: vi.fn() },
  extractErrorMessage: (err: any, fallback: string) => err?.response?.data?.error || fallback,
}));

import apiClient from '@/api/apiClient';

function mountPage() {
  return mount(PlaceDetailPage, {
    global: {
      stubs: { RouterLink: true },
    },
  });
}

function mockDetailResponse(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    data: {
      success: true,
      data: {
        id: 'place-123',
        name: '홍대 애견카페',
        category: 'CAFE',
        categoryName: '카페',
        address: '서울특별시 마포구',
        latitude: 37.5567,
        longitude: 126.9236,
        phone: '02-123-4567',
        operatingHours: '10:00-22:00',
        imageUrl: 'https://example.com/image.jpg',
        rating: 4.5,
        reviewCount: 10,
        maxWeightLimitKg: null,
        allowedSizes: ['SMALL', 'MEDIUM'],
        distanceKm: null,
        ...overrides,
      },
      error: null,
      timestamp: '2026-08-14T00:00:00',
    },
  };
}

function mockFavoritesResponse(items: Array<{ placeId: string }> = []) {
  return {
    data: {
      success: true,
      data: items.map((item, idx) => ({
        favoriteId: `fav-${idx}`,
        placeId: item.placeId,
        placeName: '홍대 애견카페',
        category: 'CAFE',
        categoryName: '카페',
        address: '서울특별시 마포구',
        imageUrl: null,
        rating: 4.5,
        reviewCount: 10,
        createdAt: '2026-08-14T00:00:00',
      })),
      error: null,
      timestamp: '2026-08-14T00:00:00',
    },
  };
}

function loginAuthStore() {
  const authStore = useAuthStore();
  authStore.setAuth('token-abc', { id: 'u1', email: 'a@a.com', nickname: '닉네임', pets: [] });
  return authStore;
}

/** apiClient.get을 경로별로 분기 처리하는 기본 mock (장소 상세 + 즐겨찾기 목록) */
function mockGetByUrl(favoritePlaceIds: string[] = []) {
  (apiClient.get as any).mockImplementation((url: string) => {
    if (url === '/users/me/favorites') {
      return Promise.resolve(mockFavoritesResponse(favoritePlaceIds.map((placeId) => ({ placeId }))));
    }
    return Promise.resolve(mockDetailResponse());
  });
}

describe('PlaceDetailPage', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    pushMock.mockClear();
    (apiClient.get as any).mockReset();
    (apiClient.post as any).mockReset();
    (apiClient.delete as any).mockReset();
  });

  it('route parameter의 id로 상세 조회 API를 호출하고 정상 응답을 화면에 표시한다', async () => {
    (apiClient.get as any).mockResolvedValue(mockDetailResponse());

    const wrapper = mountPage();
    await flushPromises();

    expect(apiClient.get).toHaveBeenCalledWith('/places/place-123');
    expect(wrapper.text()).toContain('홍대 애견카페');
    expect(wrapper.text()).toContain('서울특별시 마포구');
    expect(wrapper.text()).toContain('02-123-4567');
    expect(wrapper.text()).toContain('리뷰 10개');
    expect(wrapper.text()).toContain('소형견');
    expect(wrapper.text()).toContain('중형견');
  });

  it('로딩 중에는 로딩 스피너를 표시한다', async () => {
    let resolvePromise: (value: any) => void = () => {};
    (apiClient.get as any).mockReturnValue(
      new Promise((resolve) => {
        resolvePromise = resolve;
      })
    );

    const wrapper = mountPage();
    expect(wrapper.text()).toContain('장소 상세 정보 로딩 중');

    resolvePromise(mockDetailResponse());
    await flushPromises();
    expect(wrapper.text()).not.toContain('장소 상세 정보 로딩 중');
  });

  it('404 응답 시 장소를 찾을 수 없다는 안내를 표시한다', async () => {
    (apiClient.get as any).mockRejectedValue({ response: { status: 404 } });

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('장소 정보를 찾을 수 없습니다.');
  });

  it('404 외 API 오류 시 기존 에러 처리 패턴(ErrorMessage)으로 오류 메시지를 표시한다', async () => {
    (apiClient.get as any).mockRejectedValue({
      response: { status: 500, data: { error: '서버 오류가 발생했습니다.' } },
    });

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('서버 오류가 발생했습니다.');
    expect(wrapper.text()).not.toContain('장소 정보를 찾을 수 없습니다.');
  });

  describe('즐겨찾기', () => {
    it('로그인 사용자는 GET /users/me/favorites로 현재 장소의 즐겨찾기 상태를 조회해 표시한다', async () => {
      loginAuthStore();
      mockGetByUrl(['place-123']);

      const wrapper = mountPage();
      await flushPromises();

      expect(apiClient.get).toHaveBeenCalledWith('/users/me/favorites');
      expect(wrapper.text()).toContain('즐겨찾기됨');
    });

    it('즐겨찾기가 안 된 장소는 기본적으로 즐겨찾기 안 됨 상태로 표시된다', async () => {
      loginAuthStore();
      mockGetByUrl([]);

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.text()).toContain('즐겨찾기');
      expect(wrapper.text()).not.toContain('즐겨찾기됨');
    });

    it('즐겨찾기 추가 성공 시 POST /favorites/{placeId} 호출 후 상태가 즉시 갱신된다', async () => {
      loginAuthStore();
      mockGetByUrl([]);
      (apiClient.post as any).mockResolvedValue({ data: { success: true, data: {} } });

      const wrapper = mountPage();
      await flushPromises();

      await wrapper.find('button').trigger('click');
      await flushPromises();

      expect(apiClient.post).toHaveBeenCalledWith('/favorites/place-123');
      expect(wrapper.text()).toContain('즐겨찾기됨');
    });

    it('즐겨찾기 삭제 성공 시 DELETE /favorites/{placeId} 호출 후 상태가 즉시 갱신된다', async () => {
      loginAuthStore();
      mockGetByUrl(['place-123']);
      (apiClient.delete as any).mockResolvedValue({ data: { success: true, data: null } });

      const wrapper = mountPage();
      await flushPromises();
      expect(wrapper.text()).toContain('즐겨찾기됨');

      await wrapper.find('button').trigger('click');
      await flushPromises();

      expect(apiClient.delete).toHaveBeenCalledWith('/favorites/place-123');
      expect(wrapper.text()).not.toContain('즐겨찾기됨');
    });

    it('비로그인 사용자가 즐겨찾기 버튼을 누르면 API를 호출하지 않고 로그인 화면으로 이동한다', async () => {
      (apiClient.get as any).mockResolvedValue(mockDetailResponse());

      const wrapper = mountPage();
      await flushPromises();

      expect(apiClient.get).not.toHaveBeenCalledWith('/users/me/favorites');

      await wrapper.find('button').trigger('click');
      await flushPromises();

      expect(apiClient.post).not.toHaveBeenCalled();
      expect(apiClient.delete).not.toHaveBeenCalled();
      expect(pushMock).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/place/place-123' } });
    });

    it('즐겨찾기 추가 API 실패 시 상태를 변경하지 않고 오류 메시지를 표시한다', async () => {
      loginAuthStore();
      mockGetByUrl([]);
      (apiClient.post as any).mockRejectedValue({
        response: { status: 500, data: { error: '즐겨찾기 추가 실패' } },
      });

      const wrapper = mountPage();
      await flushPromises();

      await wrapper.find('button').trigger('click');
      await flushPromises();

      expect(wrapper.text()).not.toContain('즐겨찾기됨');
      expect(wrapper.text()).toContain('즐겨찾기 추가 실패');
    });

    it('요청이 진행 중일 때는 버튼이 비활성화되어 연속 클릭으로 중복 요청이 발생하지 않는다', async () => {
      loginAuthStore();
      mockGetByUrl([]);
      let resolvePost: (value: any) => void = () => {};
      (apiClient.post as any).mockReturnValue(
        new Promise((resolve) => {
          resolvePost = resolve;
        })
      );

      const wrapper = mountPage();
      await flushPromises();

      const button = wrapper.find('button');
      await button.trigger('click');
      await button.trigger('click');
      await button.trigger('click');

      expect(apiClient.post).toHaveBeenCalledTimes(1);
      expect(wrapper.find('button').attributes('disabled')).toBeDefined();

      resolvePost({ data: { success: true, data: {} } });
      await flushPromises();
      expect(wrapper.find('button').attributes('disabled')).toBeUndefined();
    });
  });
});
