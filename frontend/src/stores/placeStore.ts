import { defineStore } from 'pinia';
import apiClient from '@/api/apiClient';
import type { ApiResponse, PageResponse } from '@/types/api';
import type { PlaceCategory, PlaceSearchItem, PlaceSearchParams } from '@/types/place';
import type { Pet, SizeCategory } from '@/types/user';

export const SEARCH_PAGE_SIZE = 12;
export const DEFAULT_RADIUS_KM = 3;

/**
 * 장소 탐색 화면의 검색 조건과 결과 (TSK-FE-015 15-3).
 * 화면을 떠나도 남아 있어서, 장소 상세에서 뒤로 돌아오면 검색어·필터·페이지·결과가 그대로 보인다.
 * 메뉴 등으로 새로 들어올 때는 화면이 `$reset()`으로 초기화한다.
 */
export const usePlaceStore = defineStore('place', {
  state: () => ({
    keyword: '',
    category: '' as PlaceCategory | '',
    sizeCategory: '' as SizeCategory | '',
    /** 크기 필터 사용 시 허용이 확인된 장소만 볼지 여부(기본: 정보 없는 장소도 뒤에 포함) */
    sizeVerifiedOnly: false,
    radiusKm: DEFAULT_RADIUS_KM,
    distanceSortEnabled: false,
    userCoords: null as { latitude: number; longitude: number } | null,
    /** 크기 필터 기본값으로 쓴 대표 반려동물 (안내 문구용) */
    sizeDefaultPet: null as Pet | null,

    places: [] as PlaceSearchItem[],
    currentPage: 0,
    totalPages: 0,
    totalElements: 0,
    hasNext: false,
    /** 한 번이라도 검색했는지 (돌아왔을 때 결과를 다시 보여 줄 수 있는지) */
    searched: false,
    loading: false,
    errorMessage: '',
  }),
  actions: {
    /** 현재 조건으로 검색한다. 실패하면 errorMessage에 문구를 넣는다 */
    async search(page = 0) {
      this.loading = true;
      this.errorMessage = '';
      try {
        const params: PlaceSearchParams = { page, size: SEARCH_PAGE_SIZE };
        if (this.keyword) params.keyword = this.keyword;
        if (this.category) params.category = this.category;
        if (this.sizeCategory) {
          params.sizeCategory = this.sizeCategory;
          if (this.sizeVerifiedOnly) params.sizeVerifiedOnly = true;
        }
        if (this.distanceSortEnabled && this.userCoords) {
          params.latitude = this.userCoords.latitude;
          params.longitude = this.userCoords.longitude;
          params.radiusKm = this.radiusKm;
        }

        const response = await apiClient.get<ApiResponse<PageResponse<PlaceSearchItem>>>('/places/search', { params });
        if (response.data && response.data.success) {
          const pageData = response.data.data;
          this.places = pageData.content || [];
          this.currentPage = pageData.page;
          this.totalPages = pageData.totalPages;
          this.totalElements = pageData.totalElements;
          this.hasNext = pageData.hasNext;
          this.searched = true;
        }
      } catch (err) {
        console.error('Search error:', err);
        this.errorMessage = '장소 검색 중 오류가 발생했습니다.';
      } finally {
        this.loading = false;
      }
    },
  },
});
