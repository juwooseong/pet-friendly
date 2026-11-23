import { defineStore } from 'pinia';
import apiClient from '@/api/apiClient';
import type { ApiResponse } from '@/types/api';
import type { FavoriteItem, FavoriteStatus } from '@/types/favorite';

/**
 * 내 즐겨찾기 (TSK-FE-015 15-4). 즐겨찾기 화면과 장소 상세가 같은 상태를 본다.
 * - 목록을 받은 뒤(`loaded`)에는 목록이 기준이고, 받기 전에는 장소별로 확인한 값(`statusByPlace`)이 기준이다.
 * - 추가·해제는 둘 다 갱신해서, 상세에서 바꾼 결과가 즐겨찾기 화면에도 바로 맞는다.
 * 액션은 실패하면 예외를 그대로 던지며, 오류 문구는 화면이 정한다.
 */
export const useFavoriteStore = defineStore('favorite', {
  state: () => ({
    favorites: [] as FavoriteItem[],
    loaded: false,
    statusByPlace: {} as Record<string, boolean>,
  }),
  getters: {
    isFavorited: (state) => (placeId: string): boolean =>
      state.loaded ? state.favorites.some((f) => f.placeId === placeId) : !!state.statusByPlace[placeId],
  },
  actions: {
    async fetchFavorites() {
      const response = await apiClient.get<ApiResponse<FavoriteItem[]>>('/users/me/favorites');
      if (response.data && response.data.success) {
        this.favorites = Array.isArray(response.data.data) ? response.data.data : [];
        this.loaded = true;
      }
    },
    /** 장소 1곳의 즐겨찾기 여부. 목록이나 이전 확인 값이 있으면 요청하지 않는다 */
    async ensureStatus(placeId: string) {
      if (this.loaded || placeId in this.statusByPlace) return;
      const response = await apiClient.get<ApiResponse<FavoriteStatus>>(`/favorites/${placeId}`);
      if (response.data && response.data.success && response.data.data) {
        this.statusByPlace[placeId] = response.data.data.favorited;
      }
    },
    async addFavorite(placeId: string) {
      const response = await apiClient.post<ApiResponse<FavoriteItem>>(`/favorites/${placeId}`);
      this.statusByPlace[placeId] = true;
      const item = response?.data?.data;
      if (this.loaded) {
        if (item) this.favorites = [item, ...this.favorites];
        else this.loaded = false; // 응답에 항목이 없으면 다음에 목록을 다시 받는다
      }
    },
    async removeFavorite(placeId: string) {
      await apiClient.delete(`/favorites/${placeId}`);
      this.statusByPlace[placeId] = false;
      this.favorites = this.favorites.filter((f) => f.placeId !== placeId);
    },
  },
});
