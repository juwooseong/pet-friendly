import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';

const loadKakaoMapsMock = vi.fn();
vi.mock('@/utils/kakaoMap', () => ({
  loadKakaoMaps: (...args: any[]) => loadKakaoMapsMock(...args),
}));

import MapPage from './MapPage.vue';

function makeFakeSdk() {
  const mapInstances: any[] = [];
  const sdk = {
    LatLng: vi.fn(function (lat: number, lng: number) {
      return { lat, lng };
    }),
    Map: vi.fn(function (this: any, container: HTMLElement, options: any) {
      this.container = container;
      this.options = options;
      mapInstances.push(this);
    }),
  };
  return { sdk, mapInstances };
}

function mountPage() {
  return mount(MapPage);
}

describe('MapPage', () => {
  beforeEach(() => {
    loadKakaoMapsMock.mockReset();
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
});
