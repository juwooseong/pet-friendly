import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import SearchPage from './SearchPage.vue';
import { useAuthStore } from '@/stores/authStore';

const pushMock = vi.fn();

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { get: vi.fn() },
  extractErrorMessage: (_err: any, fallback: string) => fallback,
}));

import apiClient from '@/api/apiClient';

function mountPage() {
  return mount(SearchPage, {
    global: {
      stubs: { RouterLink: true },
    },
  });
}

function mockSearchResponse(overrides: Partial<{ page: number; totalPages: number; totalElements: number; hasNext: boolean; content: any[] }> = {}) {
  return {
    data: {
      success: true,
      data: {
        content: overrides.content ?? [
          {
            id: 'place-1',
            name: '홍대 애견카페',
            category: 'CAFE',
            categoryName: '카페',
            address: '서울특별시 마포구',
            latitude: 37.5567,
            longitude: 126.9236,
            phone: null,
            operatingHours: null,
            imageUrl: null,
            rating: 4.5,
            reviewCount: 10,
            maxWeightLimitKg: null,
            allowedSizes: ['SMALL', 'MEDIUM'],
            distanceKm: null,
          },
        ],
        page: overrides.page ?? 0,
        size: 12,
        totalElements: overrides.totalElements ?? 1,
        totalPages: overrides.totalPages ?? 1,
        hasNext: overrides.hasNext ?? false,
      },
      error: null,
      timestamp: '2026-08-14T00:00:00',
    },
  };
}

describe('SearchPage', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    pushMock.mockClear();
    (apiClient.get as any).mockReset();
  });

  it('마운트 시 기본 조건(page=0)으로 장소를 검색해 목록을 표시한다', async () => {
    (apiClient.get as any).mockResolvedValue(mockSearchResponse());

    const wrapper = mountPage();
    await flushPromises();

    expect(apiClient.get).toHaveBeenCalledWith(
      '/places/search',
      expect.objectContaining({ params: expect.objectContaining({ page: 0, size: 12 }) })
    );
    expect(wrapper.text()).toContain('홍대 애견카페');
    expect(wrapper.text()).toContain('총 1개의 장소가 검색되었습니다.');
  });

  it('로그인 사용자는 대표 반려동물의 sizeCategory를 크기 필터 기본값으로 사용한다', async () => {
    const authStore = useAuthStore();
    authStore.setAuth('token-abc', { id: 'u1', email: 'a@a.com', nickname: '닉네임', pets: [] });

    (apiClient.get as any).mockImplementation((url: string) => {
      if (url === '/pets') {
        return Promise.resolve({
          data: {
            success: true,
            data: [
              { id: 'p1', name: '뽀삐', sizeCategory: 'LARGE', representative: true },
              { id: 'p2', name: '초코', sizeCategory: 'SMALL', representative: false },
            ],
          },
        });
      }
      return Promise.resolve(mockSearchResponse());
    });

    mountPage();
    await flushPromises();

    const lastSearchCall = (apiClient.get as any).mock.calls.find((call: any[]) => call[0] === '/places/search');
    expect(lastSearchCall[1].params.sizeCategory).toBe('LARGE');
  });

  it('비로그인 사용자는 /pets를 호출하지 않고 크기 필터 없이 검색한다', async () => {
    (apiClient.get as any).mockResolvedValue(mockSearchResponse());

    mountPage();
    await flushPromises();

    expect(apiClient.get).not.toHaveBeenCalledWith('/pets');
    const searchCall = (apiClient.get as any).mock.calls.find((call: any[]) => call[0] === '/places/search');
    expect(searchCall[1].params.sizeCategory).toBeUndefined();
  });

  it('거리순 정렬 토글 시 geolocation 좌표를 받아 latitude/longitude/radiusKm 파라미터로 재검색한다', async () => {
    (apiClient.get as any).mockResolvedValue(mockSearchResponse());
    const getCurrentPosition = vi.fn((success: PositionCallback) => {
      success({ coords: { latitude: 37.5, longitude: 127.0 } } as GeolocationPosition);
    });
    Object.defineProperty(global.navigator, 'geolocation', {
      value: { getCurrentPosition },
      configurable: true,
    });

    const wrapper = mountPage();
    await flushPromises();

    await wrapper.find('button.px-4.py-2.rounded-xl').trigger('click');
    await flushPromises();

    const lastSearchCall = (apiClient.get as any).mock.calls.at(-1);
    expect(lastSearchCall[0]).toBe('/places/search');
    expect(lastSearchCall[1].params.latitude).toBe(37.5);
    expect(lastSearchCall[1].params.longitude).toBe(127.0);
    expect(lastSearchCall[1].params.radiusKm).toBe(3);
  });

  it('다음 페이지 버튼 클릭 시 다음 page 번호로 재검색한다', async () => {
    (apiClient.get as any).mockResolvedValue(
      mockSearchResponse({ page: 0, totalPages: 2, totalElements: 13, hasNext: true })
    );

    const wrapper = mountPage();
    await flushPromises();

    const nextButton = wrapper.findAll('button').find((btn) => btn.text() === '다음');
    expect(nextButton).toBeTruthy();
    expect(nextButton!.attributes('disabled')).toBeUndefined();

    (apiClient.get as any).mockResolvedValue(mockSearchResponse({ page: 1, totalPages: 2, totalElements: 13, hasNext: false }));
    await nextButton!.trigger('click');
    await flushPromises();

    const lastSearchCall = (apiClient.get as any).mock.calls.at(-1);
    expect(lastSearchCall[1].params.page).toBe(1);
  });
});
