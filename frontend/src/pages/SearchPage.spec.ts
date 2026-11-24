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
    authStore.setAuth('token-abc', { id: 'u1', email: 'a@a.com', nickname: '닉네임', role: 'USER' });

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

  it('상세에서 뒤로 돌아오면 검색 상태를 유지하고, 메뉴로 새로 들어오면 초기화한다 (TSK-FE-015)', async () => {
    (apiClient.get as any).mockResolvedValue(mockSearchResponse());
    const first = mountPage();
    await flushPromises();
    await first.find('input[type="text"]').setValue('애견카페');
    await first.find('button').trigger('click');
    await flushPromises();
    first.unmount();
    const callsBefore = (apiClient.get as any).mock.calls.length;

    // 뒤로 가기: Vue Router가 기록 항목에 다음 화면(forward)을 남겨 둔 상태
    window.history.replaceState({ forward: '/place/place-1' }, '');
    const back = mountPage();
    await flushPromises();
    expect((apiClient.get as any).mock.calls.length).toBe(callsBefore);
    expect((back.find('input[type="text"]').element as HTMLInputElement).value).toBe('애견카페');
    expect(back.text()).toContain('홍대 애견카페');
    back.unmount();

    // 메뉴로 새로 진입: forward 없음 → 초기 조건으로 다시 검색
    window.history.replaceState(null, '');
    const fresh = mountPage();
    await flushPromises();
    expect((fresh.find('input[type="text"]').element as HTMLInputElement).value).toBe('');
    const lastCall = (apiClient.get as any).mock.calls.at(-1);
    expect(lastCall[1].params.keyword).toBeUndefined();
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

  it('페이지 번호를 누르면 새 페이지 목록으로 바뀌고 화면 맨 위로 스크롤한다', async () => {
    const scrollSpy = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
    const placeNamed = (id: string, name: string) => ({
      id, name, category: 'PARK', categoryName: '공원', address: '주소', latitude: 37.5, longitude: 127.0,
      phone: null, operatingHours: null, imageUrl: null, rating: 0, reviewCount: 0,
      maxWeightLimitKg: null, allowedSizes: null, distanceKm: null,
    });
    (apiClient.get as any).mockResolvedValue(
      mockSearchResponse({ page: 0, totalPages: 2, totalElements: 13, hasNext: true, content: [placeNamed('p1', '첫 페이지 장소')] })
    );

    const wrapper = mountPage();
    await flushPromises();
    expect(wrapper.text()).toContain('첫 페이지 장소');

    (apiClient.get as any).mockResolvedValue(
      mockSearchResponse({ page: 1, totalPages: 2, totalElements: 13, hasNext: false, content: [placeNamed('p2', '두 번째 페이지 장소')] })
    );
    const page2Button = wrapper.findAll('button').find((btn) => btn.text() === '2');
    await page2Button!.trigger('click');
    await flushPromises();

    expect(wrapper.text()).toContain('두 번째 페이지 장소');
    expect(wrapper.text()).not.toContain('첫 페이지 장소');
    expect(scrollSpy).toHaveBeenCalledWith(expect.objectContaining({ top: 0 }));
    scrollSpy.mockRestore();
  });

  describe('크기 필터 - 정책 미확인 장소 처리 (TSK-FE-019)', () => {
    const verifiedPlace = {
      id: 'verified', name: '대형견 허용 확인 장소', category: 'PARK', categoryName: '관광지', address: '주소',
      latitude: 37.5, longitude: 127.0, phone: null, operatingHours: null, imageUrl: null, rating: 0, reviewCount: 0,
      maxWeightLimitKg: null, allowedSizes: ['SMALL', 'MEDIUM', 'LARGE'], distanceKm: null,
    };
    const unknownPlace = { ...verifiedPlace, id: 'unknown', name: '크기 정보 없는 장소', allowedSizes: null };

    function loginWithLargePet() {
      const authStore = useAuthStore();
      authStore.setAuth('token-abc', { id: 'u1', email: 'a@a.com', nickname: '닉네임', role: 'USER' });
      (apiClient.get as any).mockImplementation((url: string) => {
        if (url === '/pets') {
          return Promise.resolve({ data: { success: true, data: [{ id: 'p1', name: '뽀삐', sizeCategory: 'LARGE', representative: true }] } });
        }
        return Promise.resolve(mockSearchResponse({ content: [verifiedPlace, unknownPlace], totalElements: 2 }));
      });
    }

    const searchCalls = () => (apiClient.get as any).mock.calls.filter((c: any[]) => c[0] === '/places/search');

    it('크기 필터가 걸리면 기본으로 미확인 장소도 포함해 검색하고(sizeVerifiedOnly 미전송), 미확인 장소에만 "크기 조건 확인 필요" 배지를 붙인다', async () => {
      loginWithLargePet();

      const wrapper = mountPage();
      await flushPromises();

      expect(searchCalls().at(-1)[1].params.sizeVerifiedOnly).toBeUndefined();
      const cards = wrapper.findAll('[data-testid="place-thumbnail"]').map((t) => t.element.parentElement!);
      expect(cards[0].textContent).toContain('대형견 허용 확인 장소');
      expect(cards[0].textContent).not.toContain('크기 조건 확인 필요');
      expect(cards[1].textContent).toContain('크기 정보 없는 장소');
      expect(cards[1].textContent).toContain('크기 조건 확인 필요');
      expect(wrapper.find('[data-testid="size-filter-info"]').text()).toContain('대형견 동반이 확인된 곳만 보기');
    });

    it('"확인된 곳만 보기"를 켜면 sizeVerifiedOnly=true로 첫 페이지부터 다시 검색한다', async () => {
      loginWithLargePet();

      const wrapper = mountPage();
      await flushPromises();

      await wrapper.find('[data-testid="size-verified-only"]').setValue(true);
      await flushPromises();

      const params = searchCalls().at(-1)[1].params;
      expect(params.sizeCategory).toBe('LARGE');
      expect(params.sizeVerifiedOnly).toBe(true);
      expect(params.page).toBe(0);
    });

    it('크기 필터가 없으면 확인 필요 배지와 "확인된 곳만 보기"를 표시하지 않는다', async () => {
      (apiClient.get as any).mockResolvedValue(mockSearchResponse({ content: [unknownPlace] }));

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.find('[data-testid="size-unverified-badge"]').exists()).toBe(false);
      expect(wrapper.find('[data-testid="size-filter-info"]').exists()).toBe(false);
      expect(searchCalls().at(-1)[1].params.sizeVerifiedOnly).toBeUndefined();
    });
  });

  describe('썸네일', () => {
    const placeWith = (imageUrl: string | null) => ({
      id: 'place-thumb',
      name: '썸네일 테스트 장소',
      category: 'PARK',
      categoryName: '공원',
      address: '강원특별자치도 강릉시',
      latitude: 37.75,
      longitude: 128.9,
      phone: null,
      operatingHours: null,
      imageUrl,
      rating: 0,
      reviewCount: 0,
      maxWeightLimitKg: null,
      allowedSizes: null,
      distanceKm: null,
    });

    it('imageUrl이 있으면 카드 상단에 지연 로딩 썸네일 이미지를 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(
        mockSearchResponse({ content: [placeWith('https://tong.visitkorea.or.kr/cms/resource/38/3573038_image2_1.jpg')] })
      );

      const wrapper = mountPage();
      await flushPromises();

      const img = wrapper.find('[data-testid="place-thumbnail"] img');
      expect(img.exists()).toBe(true);
      expect(img.attributes('src')).toBe('https://tong.visitkorea.or.kr/cms/resource/38/3573038_image2_1.jpg');
      expect(img.attributes('loading')).toBe('lazy');
      expect(img.attributes('alt')).toBe('썸네일 테스트 장소');
    });

    it('imageUrl이 없으면 장소 유형에 맞는 캐릭터를 표시한다 (공원 → 콩이, 카페 → 나비)', async () => {
      (apiClient.get as any).mockResolvedValue(
        mockSearchResponse({ content: [placeWith(null), { ...placeWith(null), id: 'cafe-thumb', category: 'CAFE' }] })
      );

      const wrapper = mountPage();
      await flushPromises();

      const thumbs = wrapper.findAll('[data-testid="place-thumbnail"]');
      expect(thumbs[0].find('img:not([aria-hidden])').exists()).toBe(false);
      expect(thumbs[0].find('[data-testid="place-image-placeholder"]').attributes('data-pose')).toBe('kongi-camera');
      expect(thumbs[1].find('[data-testid="place-image-placeholder"]').attributes('data-pose')).toBe('nabi-inspect');
    });

    it('이미지 로드에 실패하면 깨진 이미지 대신 기본 아이콘으로 바꾼다', async () => {
      (apiClient.get as any).mockResolvedValue(mockSearchResponse({ content: [placeWith('https://example.com/broken.jpg')] }));

      const wrapper = mountPage();
      await flushPromises();
      await wrapper.find('[data-testid="place-thumbnail"] img').trigger('error');

      const thumb = wrapper.find('[data-testid="place-thumbnail"]');
      expect(thumb.find('img:not([aria-hidden])').exists()).toBe(false);
      expect(thumb.find('[data-testid="place-image-placeholder"]').exists()).toBe(true);
    });
  });

  describe('목록 카드 (썸네일 / 분류 / 출처)', () => {
    it('작은 썸네일(thumbnailUrl)이 있으면 원본 대신 사용하고, 분류 이름과 데이터 출처를 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(
        mockSearchResponse({
          content: [
            {
              id: 'p-1', name: '와룡공원', category: 'PARK', categoryName: '관광지', address: '서울 종로구',
              latitude: 37.59, longitude: 126.99, phone: null, operatingHours: null,
              imageUrl: 'https://tong.visitkorea.or.kr/original.jpg', thumbnailUrl: 'https://tong.visitkorea.or.kr/small.jpg',
              rating: 0, reviewCount: 0, maxWeightLimitKg: null, allowedSizes: null, distanceKm: null,
            },
          ],
        })
      );

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.find('[data-testid="place-thumbnail"] img').attributes('src')).toBe('https://tong.visitkorea.or.kr/small.jpg');
      // 작은 사진이 깨지면(관광공사 작은 사진 약 3%가 404) 원본으로 다시 시도한다 (TSK-FE-025)
      await wrapper.find('[data-testid="place-thumbnail"] img').trigger('error');
      expect(wrapper.find('[data-testid="place-thumbnail"] img').attributes('src')).toBe('https://tong.visitkorea.or.kr/original.jpg');
      expect(wrapper.text()).toContain('관광지');
      expect(wrapper.find('[data-testid="data-source"]').text()).toContain('한국관광공사');
    });

    it('카테고리 필터에 쇼핑·문화시설·축제·여행코스가 있다', async () => {
      (apiClient.get as any).mockResolvedValue(mockSearchResponse());

      const wrapper = mountPage();
      await flushPromises();

      const values = wrapper.findAll('option').map((o) => o.attributes('value'));
      expect(values).toEqual(expect.arrayContaining(['SHOPPING', 'CULTURE', 'FESTIVAL', 'COURSE']));
    });
  });
});
