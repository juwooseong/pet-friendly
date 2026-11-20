import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
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

/**
 * Kakao Maps SDK 가짜 구현.
 * withViewport=true면 지도 인스턴스가 getCenter/getBounds를 제공하고, `moveTo`로 중심·보이는 영역을 바꿀 수 있다
 * (보이는 영역은 중심에서 위도/경도 ±halfSpanDeg 사각형).
 */
function makeFakeSdk({ withViewport = false } = {}) {
  const mapInstances: any[] = [];
  const markerInstances: any[] = [];
  const clickHandlers = new WeakMap<any, () => void>();
  const idleHandlers = new Map<any, () => void>();
  const latLng = (lat: number, lng: number) => ({ lat, lng, getLat: () => lat, getLng: () => lng });
  const sdk = {
    LatLng: vi.fn(function (lat: number, lng: number) {
      return latLng(lat, lng);
    }),
    Map: vi.fn(function (this: any, container: HTMLElement, options: any) {
      this.container = container;
      this.options = options;
      if (withViewport) {
        this.center = { lat: options.center.lat, lng: options.center.lng };
        this.halfSpanDeg = 0.02;
        this.getCenter = () => latLng(this.center.lat, this.center.lng);
        this.getBounds = () => ({
          getNorthEast: () => latLng(this.center.lat + this.halfSpanDeg, this.center.lng + this.halfSpanDeg),
        });
        this.moveTo = (lat: number, lng: number, halfSpanDeg = this.halfSpanDeg) => {
          this.center = { lat, lng };
          this.halfSpanDeg = halfSpanDeg;
        };
      }
      mapInstances.push(this);
    }),
    Marker: vi.fn(function (this: any, options: any) {
      this.options = options;
      this.setMap = vi.fn();
      markerInstances.push(this);
    }),
    event: {
      addListener: vi.fn((target: any, event: string, handler: () => void) => {
        if (event === 'idle') {
          idleHandlers.set(target, handler);
        } else {
          clickHandlers.set(target, handler);
        }
      }),
      removeListener: vi.fn((target: any, event: string) => {
        if (event === 'idle') idleHandlers.delete(target);
      }),
    },
  };
  /** 지도 이동/확대·축소가 끝났을 때 Kakao가 보내는 idle 이벤트를 흉내 낸다 */
  const fireIdle = () => {
    const map = mapInstances[0];
    idleHandlers.get(map)?.();
  };
  return { sdk, mapInstances, markerInstances, clickHandlers, idleHandlers, fireIdle };
}

function mockPlacesResponse(content: any[], totalElements = content.length) {
  return {
    data: {
      success: true,
      data: {
        content,
        page: 0,
        size: 100,
        totalElements,
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

  describe('지도 이동/확대·축소 시 재검색 (TSK-FE-018)', () => {
    const searchCalls = () => (apiClient.get as any).mock.calls.filter((c: any[]) => c[0] === '/places/search');
    const lastParams = () => searchCalls().at(-1)[1].params;

    beforeEach(() => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    async function mountWithViewport() {
      const fake = makeFakeSdk({ withViewport: true });
      loadKakaoMapsMock.mockResolvedValue(fake.sdk);
      const wrapper = mountPage();
      await flushPromises();
      return { wrapper, ...fake };
    }

    async function idleAndWait(fireIdle: () => void) {
      fireIdle();
      vi.advanceTimersByTime(400);
      await flushPromises();
    }

    it('첫 검색 반경은 지도에 보이는 영역(중심~모서리 거리)으로 계산하고 최대 100곳을 요청한다', async () => {
      await mountWithViewport();

      // 중심 (37.5, 127.0), 모서리 (37.52, 127.02) → 약 2.8km
      const params = lastParams();
      expect(params.latitude).toBe(37.5);
      expect(params.longitude).toBe(127.0);
      expect(params.radiusKm).toBeGreaterThan(2.5);
      expect(params.radiusKm).toBeLessThan(3.1);
      expect(params.size).toBe(100);
    });

    it('지도 생성 직후의 idle 이벤트(이동 없음)에는 다시 검색하지 않는다', async () => {
      const { fireIdle } = await mountWithViewport();
      expect(searchCalls().length).toBe(1);

      await idleAndWait(fireIdle);

      expect(searchCalls().length).toBe(1);
    });

    it('지도를 충분히 옮기면 새 중심 기준으로 다시 검색하고 마커를 새로 그린다', async () => {
      const { fireIdle, mapInstances, markerInstances } = await mountWithViewport();
      (apiClient.get as any).mockResolvedValue(mockPlacesResponse([samplePlace, { ...samplePlace, id: 'place-2' }]));

      mapInstances[0].moveTo(35.1796, 129.0756); // 부산
      await idleAndWait(fireIdle);

      expect(searchCalls().length).toBe(2);
      expect(lastParams()).toMatchObject({ latitude: 35.1796, longitude: 129.0756 });
      expect(markerInstances.length).toBe(2);
    });

    it('조금만 움직이면(직전 반경의 25% 미만) 다시 검색하지 않는다', async () => {
      const { fireIdle, mapInstances } = await mountWithViewport();

      mapInstances[0].moveTo(37.5005, 127.0005); // 약 70m
      await idleAndWait(fireIdle);

      expect(searchCalls().length).toBe(1);
    });

    it('지도를 축소하면 넓어진 영역으로 다시 검색하되 반경은 50km를 넘지 않는다', async () => {
      const { fireIdle, mapInstances } = await mountWithViewport();

      mapInstances[0].moveTo(37.5, 127.0, 3); // 위도/경도 ±3도 → 수백 km
      await idleAndWait(fireIdle);

      expect(searchCalls().length).toBe(2);
      expect(lastParams().radiusKm).toBe(50);
    });

    it('연속으로 움직이는 동안에는 기다렸다가 마지막 위치로 한 번만 검색한다', async () => {
      const { fireIdle, mapInstances } = await mountWithViewport();

      mapInstances[0].moveTo(36.0, 127.0);
      fireIdle();
      vi.advanceTimersByTime(200);
      mapInstances[0].moveTo(35.1796, 129.0756);
      fireIdle();
      vi.advanceTimersByTime(400);
      await flushPromises();

      expect(searchCalls().length).toBe(2);
      expect(lastParams()).toMatchObject({ latitude: 35.1796, longitude: 129.0756 });
    });

    it('먼저 보낸 요청의 응답이 늦게 와도 최신 결과를 덮어쓰지 않는다', async () => {
      const { fireIdle, mapInstances, markerInstances } = await mountWithViewport();

      let resolveSlow: (v: any) => void = () => {};
      (apiClient.get as any)
        .mockReturnValueOnce(new Promise((resolve) => { resolveSlow = resolve; }))
        .mockResolvedValueOnce(mockPlacesResponse([{ ...samplePlace, id: 'busan', name: '부산 장소' }]));

      mapInstances[0].moveTo(36.0, 127.0);
      await idleAndWait(fireIdle); // 느린 요청(대전)
      mapInstances[0].moveTo(35.1796, 129.0756);
      await idleAndWait(fireIdle); // 빠른 요청(부산)

      resolveSlow(mockPlacesResponse([samplePlace, { ...samplePlace, id: 'old-2' }, { ...samplePlace, id: 'old-3' }]));
      await flushPromises();

      // 마지막으로 그려진 마커는 부산 결과 1개여야 한다
      const lastMarker = markerInstances.at(-1);
      expect(markerInstances.length).toBe(1);
      expect(lastMarker.options.position).toMatchObject({ lat: samplePlace.latitude });
    });

    it('재검색이 실패하면 기존 마커는 유지하고 오류를 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(mockPlacesResponse([samplePlace]));
      const { wrapper, fireIdle, mapInstances, markerInstances } = await mountWithViewport();
      expect(markerInstances.length).toBe(1);

      (apiClient.get as any).mockRejectedValue(new Error('network error'));
      mapInstances[0].moveTo(35.1796, 129.0756);
      await idleAndWait(fireIdle);

      expect(markerInstances[0].setMap).not.toHaveBeenCalledWith(null);
      expect(wrapper.text()).toContain('주변 장소를 불러오는 중 오류가 발생했습니다.');
    });

    it('표시한 장소 수와, 전체가 더 많으면 확대 안내를 보여준다', async () => {
      (apiClient.get as any).mockResolvedValue(mockPlacesResponse([samplePlace], 1234));
      const { wrapper } = await mountWithViewport();

      const status = wrapper.find('[data-testid="map-status"]').text();
      expect(status).toContain('이 지역 장소 1곳');
      expect(status).toContain('전체 1,234곳 중 가까운 순');
    });

    it('언마운트하면 idle 리스너를 해제하고 대기 중인 재검색을 취소한다', async () => {
      const { wrapper, fireIdle, mapInstances, sdk, idleHandlers } = await mountWithViewport();
      const map = mapInstances[0];
      const handler = idleHandlers.get(map)!;

      map.moveTo(35.1796, 129.0756);
      fireIdle(); // 디바운스 대기 중
      wrapper.unmount();
      vi.advanceTimersByTime(400);
      await flushPromises();

      expect(sdk.event.removeListener).toHaveBeenCalledWith(map, 'idle', handler);
      expect(searchCalls().length).toBe(1);
    });
  });
});
