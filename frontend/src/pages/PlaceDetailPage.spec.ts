import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import PlaceDetailPage from './PlaceDetailPage.vue';
import { useAuthStore } from '@/stores/authStore';
import { useFavoriteStore } from '@/stores/favoriteStore';

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

function mockFavoriteStatusResponse(favorited: boolean) {
  return {
    data: {
      success: true,
      data: { placeId: 'place-123', favorited },
      error: null,
      timestamp: '2026-08-14T00:00:00',
    },
  };
}

function loginAuthStore() {
  const authStore = useAuthStore();
  authStore.setAuth('token-abc', { id: 'u1', email: 'a@a.com', nickname: '닉네임', role: 'USER' });
  return authStore;
}

function mockReviewsResponse(reviews: Array<Partial<Record<string, unknown>>> = []) {
  return {
    data: {
      success: true,
      data: reviews,
      error: null,
      timestamp: '2026-08-14T00:00:00',
    },
  };
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

/** apiClient.get을 경로별로 분기 처리하는 기본 mock (장소 상세 + 즐겨찾기 목록 + 리뷰 목록) */
function mockGetByUrl(favoritePlaceIds: string[] = [], reviews: Array<Partial<Record<string, unknown>>> = []) {
  (apiClient.get as any).mockImplementation((url: string) => {
    if (url === '/favorites/place-123') {
      return Promise.resolve(mockFavoriteStatusResponse(favoritePlaceIds.includes('place-123')));
    }
    if (url === '/places/place-123/reviews') {
      return Promise.resolve(mockReviewsResponse(reviews));
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

  describe('사진', () => {
    it('imageUrl이 있으면 이미지를 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(mockDetailResponse({ imageUrl: 'https://example.com/image.jpg' }));

      const wrapper = mountPage();
      await flushPromises();

      const img = wrapper.find('img');
      expect(img.exists()).toBe(true);
      expect(img.attributes('src')).toBe('https://example.com/image.jpg');
    });

    it('imageUrl이 없으면 fallback 아이콘을 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(mockDetailResponse({ imageUrl: null }));

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.find('img:not([aria-hidden])').exists()).toBe(false);
      expect(wrapper.find('[data-testid="place-image-placeholder"]').exists()).toBe(true);
    });

    it('이미지 로드 실패 시 fallback 아이콘으로 대체된다', async () => {
      (apiClient.get as any).mockResolvedValue(mockDetailResponse({ imageUrl: 'https://example.com/broken.jpg' }));

      const wrapper = mountPage();
      await flushPromises();

      await wrapper.find('img').trigger('error');
      await flushPromises();

      expect(wrapper.find('img:not([aria-hidden])').exists()).toBe(false);
      expect(wrapper.find('[data-testid="place-image-placeholder"]').exists()).toBe(true);
    });
  });

  describe('반려동물 동반 수칙', () => {
    it('allowedSizes와 maxWeightLimitKg이 있으면 함께 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(
        mockDetailResponse({ allowedSizes: ['SMALL', 'LARGE'], maxWeightLimitKg: 15 })
      );

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.text()).toContain('반려동물 동반 수칙');
      expect(wrapper.text()).toContain('소형견');
      expect(wrapper.text()).toContain('대형견');
      expect(wrapper.text()).toContain('최대 체중 15kg까지 동반 가능');
    });

    it('동반 수칙 관련 정보가 전혀 없으면 안내 문구를 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(
        mockDetailResponse({ allowedSizes: [], maxWeightLimitKg: null })
      );

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.text()).toContain('등록된 반려동물 동반 수칙 정보가 없습니다.');
    });
  });

  describe('즐겨찾기', () => {
    it('로그인 사용자는 GET /favorites/{placeId}로 현재 장소의 즐겨찾기 상태를 조회해 표시한다', async () => {
      loginAuthStore();
      mockGetByUrl(['place-123']);

      const wrapper = mountPage();
      await flushPromises();

      expect(apiClient.get).toHaveBeenCalledWith('/favorites/place-123');
      expect(apiClient.get).not.toHaveBeenCalledWith('/users/me/favorites');
      expect(wrapper.text()).toContain('즐겨찾기됨');
    });

    it('즐겨찾기 화면에서 받은 목록이 있으면 상태를 다시 요청하지 않고 그대로 쓴다 (TSK-FE-015)', async () => {
      loginAuthStore();
      useFavoriteStore().$patch({ favorites: [{ placeId: 'place-123' } as any], loaded: true });
      mockGetByUrl();

      const wrapper = mountPage();
      await flushPromises();

      expect(apiClient.get).not.toHaveBeenCalledWith('/favorites/place-123');
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

      expect(apiClient.get).not.toHaveBeenCalledWith('/favorites/place-123');

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

  describe('리뷰', () => {
    it('GET /places/{placeId}/reviews로 리뷰 목록을 조회해 표시한다', async () => {
      mockGetByUrl([], [makeReview({ userNickname: '멍멍이집사', content: '너무 좋았어요!' })]);

      const wrapper = mountPage();
      await flushPromises();

      expect(apiClient.get).toHaveBeenCalledWith('/places/place-123/reviews');
      expect(wrapper.text()).toContain('멍멍이집사');
      expect(wrapper.text()).toContain('너무 좋았어요!');
    });

    it('리뷰가 없으면 EmptyState 형태의 안내를 표시한다', async () => {
      mockGetByUrl([], []);

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.text()).toContain('아직 작성된 리뷰가 없습니다.');
    });

    it('비로그인 사용자에게는 작성 폼 대신 로그인 안내를 표시한다', async () => {
      mockGetByUrl([], []);

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.text()).toContain('리뷰를 작성하려면 로그인이 필요합니다.');
      expect(wrapper.find('textarea').exists()).toBe(false);
    });

    it('평점 또는 내용이 비어있으면 제출 API를 호출하지 않고 안내 메시지를 표시한다', async () => {
      loginAuthStore();
      mockGetByUrl([], []);

      const wrapper = mountPage();
      await flushPromises();

      const buttons = wrapper.findAll('button');
      const submitButton = buttons.find((b) => b.text() === '리뷰 등록')!;
      await submitButton.trigger('click');
      await flushPromises();

      expect(apiClient.post).not.toHaveBeenCalledWith('/reviews', expect.anything());
      expect(wrapper.text()).toContain('평점과 리뷰 내용을 모두 입력해주세요.');
    });

    it('로그인 사용자가 리뷰 작성에 성공하면 목록과 장소 평점/리뷰 수가 즉시 갱신된다', async () => {
      loginAuthStore();
      let reviewsCallCount = 0;
      (apiClient.get as any).mockImplementation((url: string) => {
        if (url === '/favorites/place-123') {
          return Promise.resolve(mockFavoriteStatusResponse(false));
        }
        if (url === '/places/place-123/reviews') {
          reviewsCallCount += 1;
          if (reviewsCallCount === 1) return Promise.resolve(mockReviewsResponse([]));
          return Promise.resolve(mockReviewsResponse([makeReview()]));
        }
        if (reviewsCallCount >= 1) {
          return Promise.resolve(mockDetailResponse({ rating: 5.0, reviewCount: 11 }));
        }
        return Promise.resolve(mockDetailResponse());
      });
      (apiClient.post as any).mockResolvedValue({ data: { success: true, data: makeReview() } });

      const wrapper = mountPage();
      await flushPromises();

      const stars = wrapper.findAll('button').filter((b) => b.text() === '★');
      await stars[4].trigger('click');
      await wrapper.find('textarea').setValue('정말 좋았어요');

      const submitButton = wrapper.findAll('button').find((b) => b.text() === '리뷰 등록')!;
      await submitButton.trigger('click');
      await flushPromises();

      expect(apiClient.post).toHaveBeenCalledWith('/reviews', {
        placeId: 'place-123',
        rating: 5,
        content: '정말 좋았어요',
      });
      expect(apiClient.get).toHaveBeenCalledWith('/places/place-123/reviews');
      expect(wrapper.text()).toContain('리뷰 11개');
      expect(wrapper.text()).toContain('너무 좋았어요!');
    });

    it('이미 리뷰를 작성한 경우(409) 중복 작성 오류 메시지를 표시한다', async () => {
      loginAuthStore();
      mockGetByUrl([], []);
      (apiClient.post as any).mockRejectedValue({
        response: { status: 409, data: { error: '이미 이 장소에 리뷰를 작성하셨습니다.' } },
      });

      const wrapper = mountPage();
      await flushPromises();

      const stars = wrapper.findAll('button').filter((b) => b.text() === '★');
      await stars[3].trigger('click');
      await wrapper.find('textarea').setValue('중복 작성 시도');

      const submitButton = wrapper.findAll('button').find((b) => b.text() === '리뷰 등록')!;
      await submitButton.trigger('click');
      await flushPromises();

      expect(wrapper.text()).toContain('이미 이 장소에 리뷰를 작성하셨습니다.');
    });

    it('리뷰 등록 API 실패 시 기존 에러 처리 패턴으로 오류 메시지를 표시한다', async () => {
      loginAuthStore();
      mockGetByUrl([], []);
      (apiClient.post as any).mockRejectedValue({
        response: { status: 500, data: { error: '리뷰 등록 중 서버 오류가 발생했습니다.' } },
      });

      const wrapper = mountPage();
      await flushPromises();

      const stars = wrapper.findAll('button').filter((b) => b.text() === '★');
      await stars[2].trigger('click');
      await wrapper.find('textarea').setValue('오류 테스트');

      const submitButton = wrapper.findAll('button').find((b) => b.text() === '리뷰 등록')!;
      await submitButton.trigger('click');
      await flushPromises();

      expect(wrapper.text()).toContain('리뷰 등록 중 서버 오류가 발생했습니다.');
    });
  });

  describe('장소 소개 / 운영 시간', () => {
    it('소개와 운영 시간이 있으면 줄바꿈을 유지한 텍스트로 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(
        mockDetailResponse({
          description: '반려견과 함께 걷기 좋은 숲길입니다.\n주차장이 넓습니다.',
          operatingHours: '이용 시간: 09:00~18:00\n쉬는 날: 매주 월요일',
        })
      );

      const wrapper = mountPage();
      await flushPromises();

      const description = wrapper.find('[data-testid="place-description"]');
      expect(description.exists()).toBe(true);
      expect(description.text()).toContain('장소 소개');
      expect(description.find('p').text()).toBe('반려견과 함께 걷기 좋은 숲길입니다.\n주차장이 넓습니다.');
      expect(description.find('p').classes()).toContain('whitespace-pre-line');

      const hours = wrapper.find('[data-testid="place-operating-hours"]');
      expect(hours.exists()).toBe(true);
      expect(hours.text()).toContain('쉬는 날: 매주 월요일');
    });

    it('소개에 HTML 태그 문자열이 있어도 태그로 해석하지 않고 글자 그대로 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(mockDetailResponse({ description: '<img src=x onerror=alert(1)>소개' }));

      const wrapper = mountPage();
      await flushPromises();

      const description = wrapper.find('[data-testid="place-description"]');
      expect(description.find('img').exists()).toBe(false);
      expect(description.text()).toContain('<img src=x onerror=alert(1)>소개');
    });

    it('소개와 운영 시간이 없으면 해당 영역을 표시하지 않는다', async () => {
      (apiClient.get as any).mockResolvedValue(mockDetailResponse({ description: null, operatingHours: null }));

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.find('[data-testid="place-description"]').exists()).toBe(false);
      expect(wrapper.find('[data-testid="place-operating-hours"]').exists()).toBe(false);
    });
  });

  describe('공공데이터 상세 정보 (갤러리 / 출처 / 부가 정보 / 반려견 편의 / 길찾기)', () => {
    const publicPlace = (overrides: Partial<Record<string, unknown>> = {}) =>
      mockDetailResponse({
        publicDataId: '1019041',
        name: '와룡공원',
        latitude: 37.59,
        longitude: 126.99,
        imageUrl: 'https://tong.visitkorea.or.kr/main.jpg',
        imageCopyrightType: 'Type1',
        galleryImages: [
          { url: 'https://tong.visitkorea.or.kr/1.jpg', thumbnailUrl: 'https://tong.visitkorea.or.kr/1s.jpg', copyrightType: 'Type1' },
          { url: 'https://tong.visitkorea.or.kr/2.jpg', thumbnailUrl: 'https://tong.visitkorea.or.kr/2s.jpg', copyrightType: 'Type3' },
        ],
        ...overrides,
      });

    it('상세 사진이 여러 장이면 썸네일을 눌러 큰 사진을 바꾸고, 사진의 공공누리 유형과 출처를 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(publicPlace());

      const wrapper = mountPage();
      await flushPromises();

      const gallery = wrapper.find('[data-testid="place-gallery"]');
      expect(gallery.find('img').attributes('src')).toBe('https://tong.visitkorea.or.kr/1.jpg');
      expect(wrapper.find('[data-testid="image-source"]').text()).toContain('한국관광공사');
      expect(wrapper.find('[data-testid="image-source"]').text()).toContain('공공누리 제1유형(출처표시)');

      await gallery.find('button[aria-label="사진 2"]').trigger('click');

      expect(gallery.find('img').attributes('src')).toBe('https://tong.visitkorea.or.kr/2.jpg');
      expect(wrapper.find('[data-testid="image-source"]').text()).toContain('공공누리 제3유형(출처표시·변경금지)');
    });

    it('직접 등록한 장소(publicDataId 없음)에는 한국관광공사 출처를 표시하지 않는다', async () => {
      (apiClient.get as any).mockResolvedValue(mockDetailResponse());

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.find('[data-testid="image-source"]').exists()).toBe(false);
    });

    it('주차·문의·유모차·요금과 홈페이지(http/https만)를 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(
        publicPlace({
          detailInfo: { homepage: 'https://park.example.com', parking: '가능', infoCenter: '02-111-2222', useFee: '무료' },
        })
      );

      const wrapper = mountPage();
      await flushPromises();

      const info = wrapper.find('[data-testid="place-extra-info"]');
      expect(info.text()).toContain('주차');
      expect(info.text()).toContain('02-111-2222');
      expect(info.text()).toContain('무료');
      const link = info.find('a');
      expect(link.attributes('href')).toBe('https://park.example.com');
      expect(link.attributes('rel')).toBe('noopener noreferrer');
    });

    it('http/https가 아닌 홈페이지 값은 링크로 만들지 않는다', async () => {
      (apiClient.get as any).mockResolvedValue(publicPlace({ detailInfo: { homepage: 'javascript:alert(1)' } }));

      const wrapper = mountPage();
      await flushPromises();

      expect(wrapper.find('[data-testid="place-extra-info"]').exists()).toBe(false);
      expect(wrapper.html()).not.toContain('javascript:alert(1)');
    });

    it('반려동물 동반 조건 배지와 반려견 편의시설을 근거가 있는 항목만 표시한다', async () => {
      (apiClient.get as any).mockResolvedValue(
        publicPlace({
          allowedSizes: null,
          petPolicyDetail: {
            accompanyScope: 'PARTIAL_AREAS',
            leashRequired: true,
            vaccinationRequired: true,
            maxPetCount: 2,
            etcText: '예방 접종이 완료된 반려견만 동반 가능합니다.',
            facilities: '잔디구장, 산책로',
            rentalItems: '목줄',
          },
        })
      );

      const wrapper = mountPage();
      await flushPromises();

      const badges = wrapper.find('[data-testid="pet-requirements"]').text();
      expect(badges).toContain('일부 구역만 동반 가능');
      expect(badges).toContain('목줄 필수');
      expect(badges).toContain('예방접종 필수');
      expect(badges).toContain('최대 2마리');
      expect(badges).not.toContain('이동장'); // 근거 없는 항목은 표시하지 않는다
      expect(wrapper.text()).toContain('예방 접종이 완료된 반려견만 동반 가능합니다.');

      const amenities = wrapper.find('[data-testid="pet-amenities"]').text();
      expect(amenities).toContain('잔디구장, 산책로');
      expect(amenities).toContain('대여 가능');
      expect(amenities).not.toContain('구매 가능');
    });

    it('제3유형 사진은 자르지 않고(object-contain), 제1유형 사진은 틀에 맞게 자른다 (TSK-FE-021)', async () => {
      (apiClient.get as any).mockResolvedValue(publicPlace()); // 1번 Type1, 2번 Type3

      const wrapper = mountPage();
      await flushPromises();

      const main = () => wrapper.find('[data-testid="gallery-main-image"]');
      expect(main().classes()).toContain('object-cover');

      await wrapper.find('button[aria-label="사진 2"]').trigger('click');
      expect(main().classes()).toContain('object-contain');
      expect(main().classes()).not.toContain('object-cover');
      expect(wrapper.find('button[aria-label="사진 2"] img').classes()).toContain('object-contain');
    });

    it('실내 출입 여부는 확인된 값만 배지로 표시한다 (TSK-BE-031)', async () => {
      (apiClient.get as any).mockResolvedValue(publicPlace({ allowedSizes: null, isIndoorAllowed: false }));
      const blocked = mountPage();
      await flushPromises();
      expect(blocked.find('[data-testid="pet-requirements"]').text()).toContain('실내 출입 불가');

      (apiClient.get as any).mockResolvedValue(publicPlace({ allowedSizes: null, isIndoorAllowed: true }));
      const allowed = mountPage();
      await flushPromises();
      expect(allowed.find('[data-testid="pet-requirements"]').text()).toContain('실내 동반 가능');

      (apiClient.get as any).mockResolvedValue(publicPlace({ allowedSizes: null, isIndoorAllowed: null }));
      const unknown = mountPage();
      await flushPromises();
      expect(unknown.text()).not.toContain('실내 출입 불가');
      expect(unknown.text()).not.toContain('실내 동반 가능');
    });

    it('카카오맵 길찾기 링크를 장소명과 좌표로 만든다', async () => {
      (apiClient.get as any).mockResolvedValue(publicPlace());

      const wrapper = mountPage();
      await flushPromises();

      const link = wrapper.find('[data-testid="kakao-directions"]');
      expect(link.attributes('href')).toBe('https://map.kakao.com/link/to/%EC%99%80%EB%A3%A1%EA%B3%B5%EC%9B%90,37.59,126.99');
      expect(link.attributes('target')).toBe('_blank');
    });
  });
});
