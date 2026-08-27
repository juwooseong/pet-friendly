import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';

const pushMock = vi.fn();

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { get: vi.fn() },
  extractErrorMessage: (err: any, fallback: string) => err?.response?.data?.error || fallback,
}));

const loadKakaoMapsMock = vi.fn();
vi.mock('@/utils/kakaoMap', () => ({
  loadKakaoMaps: (...args: any[]) => loadKakaoMapsMock(...args),
}));

import apiClient from '@/api/apiClient';
import MapPage from './MapPage.vue';

function makeFakeSdk() {
  const listeners: Array<{ marker: any; event: string; handler: () => void }> = [];
  const markers: any[] = [];
  const sdk = {
    LatLng: vi.fn(function (lat: number, lng: number) {
      return { lat, lng };
    }),
    Map: vi.fn(function (this: any, container: HTMLElement, options: any) {
      this.container = container;
      this.options = options;
    }),
    Marker: vi.fn(function (this: any, options: any) {
      this.options = options;
      this.setMap = vi.fn();
      markers.push(this);
    }),
    event: {
      addListener: vi.fn((marker: any, event: string, handler: () => void) => {
        listeners.push({ marker, event, handler });
      }),
    },
  };
  return { sdk, listeners, markers };
}

function makePlace(overrides: Partial<Record<string, unknown>> = {}) {
  return {
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
    allowedSizes: ['SMALL'],
    distanceKm: null,
    ...overrides,
  };
}

function mockPlacesResponse(content: Array<Record<string, unknown>> = []) {
  return {
    data: {
      success: true,
      data: { content, page: 0, size: 50, totalElements: content.length, totalPages: 1, hasNext: false },
      error: null,
      timestamp: '2026-08-14T00:00:00',
    },
  };
}

function mountPage() {
  return mount(MapPage, { global: { stubs: { RouterLink: true } } });
}

describe('MapPage', () => {
  beforeEach(() => {
    pushMock.mockClear();
    loadKakaoMapsMock.mockReset();
    (apiClient.get as any).mockReset();
    Object.defineProperty(global.navigator, 'geolocation', {
      value: {
        getCurrentPosition: vi.fn((success: PositionCallback) => {
          success({ coords: { latitude: 37.5, longitude: 127.0 } } as GeolocationPosition);
        }),
      },
      configurable: true,
    });
  });

  it('Kakao SDK 로드 실패(API 키 미설정 등) 시 오류 메시지를 표시하고 장소 검색을 하지 않는다', async () => {
    loadKakaoMapsMock.mockRejectedValue(new Error('Kakao Maps API 키가 설정되지 않았습니다.'));

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('Kakao Maps API 키가 설정되지 않았습니다.');
    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('Kakao SDK 로드 성공 시 지도를 초기화하고 장소 마커를 표시한다', async () => {
    const { sdk, markers } = makeFakeSdk();
    loadKakaoMapsMock.mockResolvedValue(sdk);
    (apiClient.get as any).mockResolvedValue(mockPlacesResponse([makePlace()]));

    const wrapper = mountPage();
    await flushPromises();

    expect(sdk.Map).toHaveBeenCalledTimes(1);
    expect(apiClient.get).toHaveBeenCalledWith(
      '/places/search',
      expect.objectContaining({ params: expect.objectContaining({ latitude: 37.5, longitude: 127.0, radiusKm: 5 }) })
    );
    expect(markers.length).toBe(1);
    expect(wrapper.text()).not.toContain('주변에 표시할 장소가 없습니다.');
  });

  it('장소 목록이 비어있으면 안내 문구를 표시한다', async () => {
    const { sdk } = makeFakeSdk();
    loadKakaoMapsMock.mockResolvedValue(sdk);
    (apiClient.get as any).mockResolvedValue(mockPlacesResponse([]));

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('주변에 표시할 장소가 없습니다.');
  });

  it('장소 검색 API 오류 시 오류 메시지를 표시한다', async () => {
    const { sdk } = makeFakeSdk();
    loadKakaoMapsMock.mockResolvedValue(sdk);
    (apiClient.get as any).mockRejectedValue({
      response: { status: 500, data: { error: '서버 오류가 발생했습니다.' } },
    });

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('서버 오류가 발생했습니다.');
  });

  it('마커 클릭 시 장소 정보 카드가 표시되고, 상세보기 클릭 시 장소 상세 페이지로 이동한다', async () => {
    const { sdk, listeners } = makeFakeSdk();
    loadKakaoMapsMock.mockResolvedValue(sdk);
    (apiClient.get as any).mockResolvedValue(mockPlacesResponse([makePlace()]));

    const wrapper = mountPage();
    await flushPromises();

    expect(listeners.length).toBe(1);
    listeners[0].handler();
    await flushPromises();

    expect(wrapper.text()).toContain('홍대 애견카페');

    const detailButton = wrapper.findAll('button').find((b) => b.text().includes('상세보기'))!;
    await detailButton.trigger('click');

    expect(pushMock).toHaveBeenCalledWith({ name: 'PlaceDetail', params: { id: 'place-1' } });
  });

  it('현재 위치 접근이 거부되면 기본 위치로 검색하고 안내 메시지를 표시한다', async () => {
    Object.defineProperty(global.navigator, 'geolocation', {
      value: {
        getCurrentPosition: vi.fn((_success: PositionCallback, error: PositionErrorCallback) => {
          error({} as GeolocationPositionError);
        }),
      },
      configurable: true,
    });
    const { sdk } = makeFakeSdk();
    loadKakaoMapsMock.mockResolvedValue(sdk);
    (apiClient.get as any).mockResolvedValue(mockPlacesResponse([]));

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('위치 정보 접근이 거부되어');
    const searchCall = (apiClient.get as any).mock.calls[0];
    expect(searchCall[1].params.latitude).toBe(37.5665);
    expect(searchCall[1].params.longitude).toBe(126.978);
  });

  it('브라우저가 위치 정보를 지원하지 않으면 기본 위치로 검색하고 안내 메시지를 표시한다', async () => {
    delete (global.navigator as any).geolocation;
    const { sdk } = makeFakeSdk();
    loadKakaoMapsMock.mockResolvedValue(sdk);
    (apiClient.get as any).mockResolvedValue(mockPlacesResponse([]));

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('이 브라우저는 위치 정보를 지원하지');
  });
});
