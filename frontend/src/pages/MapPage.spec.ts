import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';

const loadKakaoMapsMock = vi.fn();
vi.mock('@/utils/kakaoMap', () => ({
  loadKakaoMaps: (...args: any[]) => loadKakaoMapsMock(...args),
}));

const pushMock = vi.fn();
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { get: vi.fn() },
}));

import apiClient from '@/api/apiClient';
import MapPage from './MapPage.vue';

function makeFakeSdk() {
  const mapInstances: any[] = [];
  const markerInstances: any[] = [];
  const clickHandlers = new WeakMap<any, () => void>();
  const sdk = {
    LatLng: vi.fn(function (lat: number, lng: number) {
      return { lat, lng };
    }),
    Map: vi.fn(function (this: any, container: HTMLElement, options: any) {
      this.container = container;
      this.options = options;
      mapInstances.push(this);
    }),
    Marker: vi.fn(function (this: any, options: any) {
      this.options = options;
      this.setMap = vi.fn();
      markerInstances.push(this);
    }),
    event: {
      addListener: vi.fn((marker: any, _event: string, handler: () => void) => {
        clickHandlers.set(marker, handler);
      }),
    },
  };
  return { sdk, mapInstances, markerInstances, clickHandlers };
}

function mockPlacesResponse(content: any[]) {
  return {
    data: {
      success: true,
      data: {
        content,
        page: 0,
        size: 20,
        totalElements: content.length,
        totalPages: 1,
        hasNext: false,
      },
      error: null,
      timestamp: '2026-09-09T00:00:00',
    },
  };
}

const samplePlace = {
  id: 'place-1',
  publicDataId: undefined,
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
};

function mountPage() {
  return mount(MapPage);
}

describe('MapPage', () => {
  beforeEach(() => {
    loadKakaoMapsMock.mockReset();
    pushMock.mockClear();
    (apiClient.get as any).mockReset();
    (apiClient.get as any).mockResolvedValue(mockPlacesResponse([]));
    Object.defineProperty(global.navigator, 'geolocation', {
      value: {
        getCurrentPosition: vi.fn((success: PositionCallback) => {
          success({ coords: { latitude: 37.5, longitude: 127.0 } } as GeolocationPosition);
        }),
      },
      configurable: true,
    });
  });

  it('마운트 시 로딩 상태를 표시한 뒤 Kakao SDK 로드를 시도한다', async () => {
    let resolveLoad: (value: any) => void = () => {};
    loadKakaoMapsMock.mockReturnValue(
      new Promise((resolve) => {
        resolveLoad = resolve;
      })
    );

    const wrapper = mountPage();
    expect(wrapper.text()).toContain('지도를 불러오는 중');

    const { sdk } = makeFakeSdk();
    resolveLoad(sdk);
    await flushPromises();

    expect(loadKakaoMapsMock).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).not.toContain('지도를 불러오는 중');
  });

  it('Kakao SDK 로드 성공 시 지도 컨테이너를 렌더링하고 kakao.maps.Map을 초기화한다', async () => {
    const { sdk, mapInstances } = makeFakeSdk();
    loadKakaoMapsMock.mockResolvedValue(sdk);

    const wrapper = mountPage();
    await flushPromises();

    expect(sdk.Map).toHaveBeenCalledTimes(1);
    expect(mapInstances.length).toBe(1);
    expect(wrapper.find('.h-\\[500px\\]').exists()).toBe(true);
  });

  it('Kakao SDK 로드 실패(API 키 미설정 등) 시 오류 메시지를 표시하고 지도를 렌더링하지 않는다', async () => {
    loadKakaoMapsMock.mockRejectedValue(new Error('Kakao Maps API 키가 설정되지 않았습니다.'));

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('Kakao Maps API 키가 설정되지 않았습니다.');
    expect(wrapper.find('.h-\\[500px\\]').exists()).toBe(false);
  });

  it('현재 위치 권한이 허용되면 현재 위치 좌표로 지도를 초기화한다', async () => {
    const { sdk } = makeFakeSdk();
    loadKakaoMapsMock.mockResolvedValue(sdk);

    mountPage();
    await flushPromises();

    expect(sdk.LatLng).toHaveBeenCalledWith(37.5, 127.0);
  });

  it('현재 위치 접근이 거부되면 기본 좌표(서울시청)로 지도를 초기화하고 안내 메시지를 표시한다', async () => {
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

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('위치 정보 접근이 거부되어');
    expect(sdk.LatLng).toHaveBeenCalledWith(37.5665, 126.978);
    // 거부 시에도 기본 좌표로 장소 검색이 계속 진행되어야 한다.
    expect(apiClient.get).toHaveBeenCalledWith(
      '/places/search',
      expect.objectContaining({ params: expect.objectContaining({ latitude: 37.5665, longitude: 126.978 }) })
    );
  });

  it('브라우저가 위치 정보를 지원하지 않으면 기본 좌표(서울시청)로 지도를 초기화하고 안내 메시지를 표시한다', async () => {
    delete (global.navigator as any).geolocation;
    const { sdk } = makeFakeSdk();
    loadKakaoMapsMock.mockResolvedValue(sdk);

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('이 브라우저는 위치 정보를 지원하지');
    expect(sdk.LatLng).toHaveBeenCalledWith(37.5665, 126.978);
  });

  it('SDK 로드가 끝나기 전에 언마운트되어도 오류 없이 정리되고, 이후 로드가 완료돼도 상태를 갱신하지 않는다', async () => {
    let resolveLoad: (value: any) => void = () => {};
    loadKakaoMapsMock.mockReturnValue(
      new Promise((resolve) => {
        resolveLoad = resolve;
      })
    );

    const wrapper = mountPage();
    expect(() => wrapper.unmount()).not.toThrow();

    const { sdk } = makeFakeSdk();
    expect(() => resolveLoad(sdk)).not.toThrow();
    await flushPromises();
  });

  describe('장소 마커 연동', () => {
    it('지도 초기화 후 지도 중심 좌표를 기준으로 장소 검색 API를 호출한다', async () => {
      const { sdk } = makeFakeSdk();
      loadKakaoMapsMock.mockResolvedValue(sdk);

      mountPage();
      await flushPromises();

      expect(apiClient.get).toHaveBeenCalledWith(
        '/places/search',
        expect.objectContaining({
          params: expect.objectContaining({ latitude: 37.5, longitude: 127.0, radiusKm: 5, page: 0 }),
        })
      );
    });

    it('API 응답 장소 수만큼 Kakao 마커를 생성한다', async () => {
      const { sdk, markerInstances } = makeFakeSdk();
      loadKakaoMapsMock.mockResolvedValue(sdk);
      (apiClient.get as any).mockResolvedValue(mockPlacesResponse([samplePlace, { ...samplePlace, id: 'place-2', name: '망원 애견공원' }]));

      mountPage();
      await flushPromises();

      expect(markerInstances.length).toBe(2);
    });

    it('마커를 클릭하면 장소명/카테고리/평점/리뷰수와 상세보기 버튼이 표시된다', async () => {
      const { sdk, markerInstances, clickHandlers } = makeFakeSdk();
      loadKakaoMapsMock.mockResolvedValue(sdk);
      (apiClient.get as any).mockResolvedValue(mockPlacesResponse([samplePlace]));

      const wrapper = mountPage();
      await flushPromises();

      const marker = markerInstances[0];
      clickHandlers.get(marker)!();
      await flushPromises();

      expect(wrapper.text()).toContain('홍대 애견카페');
      expect(wrapper.text()).toContain('CAFE');
      expect(wrapper.text()).toContain('4.5');
      expect(wrapper.text()).toContain('리뷰 10개');
      expect(wrapper.find('button').exists()).toBe(true);
      expect(wrapper.text()).toContain('상세보기');
    });

    it('상세보기 버튼 클릭 시 /place/:id로 이동한다', async () => {
      const { sdk, markerInstances, clickHandlers } = makeFakeSdk();
      loadKakaoMapsMock.mockResolvedValue(sdk);
      (apiClient.get as any).mockResolvedValue(mockPlacesResponse([samplePlace]));

      const wrapper = mountPage();
      await flushPromises();

      clickHandlers.get(markerInstances[0])!();
      await flushPromises();

      const detailButton = wrapper.findAll('button').find((btn) => btn.text() === '상세보기');
      await detailButton!.trigger('click');

      expect(pushMock).toHaveBeenCalledWith('/place/place-1');
    });

    it('장소가 0건이면 지도는 유지하고 안내 문구를 표시한다', async () => {
      const { sdk } = makeFakeSdk();
      loadKakaoMapsMock.mockResolvedValue(sdk);
      (apiClient.get as any).mockResolvedValue(mockPlacesResponse([]));

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.find('.h-\\[500px\\]').exists()).toBe(true);
      expect(wrapper.text()).toContain('주변에 등록된 장소가 없습니다.');
    });

    it('장소 검색 API 오류 시 지도는 유지하고 오류 안내를 표시한다', async () => {
      const { sdk } = makeFakeSdk();
      loadKakaoMapsMock.mockResolvedValue(sdk);
      (apiClient.get as any).mockRejectedValue(new Error('network error'));

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.find('.h-\\[500px\\]').exists()).toBe(true);
      expect(wrapper.text()).toContain('주변 장소를 불러오는 중 오류가 발생했습니다.');
    });
  });
});
