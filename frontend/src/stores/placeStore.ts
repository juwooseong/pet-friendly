import { defineStore } from 'pinia';
import type { PlaceCategory, PlaceSearchItem } from '@/types/place';

export const usePlaceStore = defineStore('place', {
  state: () => ({
    places: [] as PlaceSearchItem[],
    selectedCategory: 'all' as PlaceCategory | 'all',
    searchKeyword: '',
    dogSizeFilter: 'all' as 'all' | 'SMALL' | 'MEDIUM' | 'LARGE',
    indoorOnlyFilter: false,
    offLeashOnlyFilter: false,
    selectedPlaceId: null as string | null
  }),
  getters: {
    selectedPlace: (state): PlaceSearchItem | null => {
      return state.places.find(p => p.id === state.selectedPlaceId) || null;
    }
  },
  actions: {
    setPlaces(places: PlaceSearchItem[]) {
      this.places = places;
    },
    setCategory(category: PlaceCategory | 'all') {
      this.selectedCategory = category;
    },
    setSearchKeyword(keyword: string) {
      this.searchKeyword = keyword;
    },
    selectPlace(placeId: string | null) {
      this.selectedPlaceId = placeId;
    }
  }
});
