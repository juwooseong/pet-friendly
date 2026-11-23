import { defineStore } from 'pinia';
import apiClient from '@/api/apiClient';
import type { ApiResponse } from '@/types/api';
import type { Pet, PetCreateRequest, PetUpdateRequest } from '@/types/user';

/**
 * 내 반려동물 목록 (TSK-FE-015). 화면은 API를 직접 부르지 않고 이 스토어의 액션을 쓴다.
 * 대표 반려동물은 서버의 `representative` 값을 그대로 따른다(별도 선택 상태를 두지 않음).
 * 액션은 실패하면 예외를 그대로 던지며, 오류 문구는 화면이 정한다.
 */
export const usePetStore = defineStore('pet', {
  state: () => ({
    pets: [] as Pet[],
    /** 한 번이라도 서버에서 목록을 받았는지 (ensurePets 중복 조회 방지) */
    loaded: false,
  }),
  getters: {
    /** 대표 반려동물. 지정된 것이 없으면 첫 번째 */
    representativePet: (state): Pet | null =>
      state.pets.find((p) => p.representative) || state.pets[0] || null,
  },
  actions: {
    async fetchPets() {
      const response = await apiClient.get<ApiResponse<Pet[]>>('/pets');
      if (response.data && response.data.success) {
        this.pets = Array.isArray(response.data.data) ? response.data.data : [];
        this.loaded = true;
      }
    },
    /** 아직 받은 적이 없을 때만 조회한다 (다른 화면에서 이미 받았으면 재사용) */
    async ensurePets() {
      if (!this.loaded) await this.fetchPets();
    },
    async createPet(payload: PetCreateRequest) {
      await apiClient.post('/pets', payload);
      await this.fetchPets();
    },
    async updatePet(petId: string, payload: PetUpdateRequest) {
      await apiClient.put(`/pets/${petId}`, payload);
      await this.fetchPets();
    },
    async deletePet(petId: string) {
      await apiClient.delete(`/pets/${petId}`);
      await this.fetchPets();
    },
    async setRepresentative(petId: string) {
      await apiClient.patch(`/pets/${petId}/representative`);
      await this.fetchPets();
    },
  },
});
