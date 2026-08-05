import { defineStore } from 'pinia';
import type { Pet } from '@/types/user';

export const usePetStore = defineStore('pet', {
  state: () => ({
    pets: [] as Pet[],
    activePetId: localStorage.getItem('petspot_active_pet_id') || ''
  }),
  getters: {
    activePet: (state): Pet | null => {
      if (!state.pets.length) return null;
      return state.pets.find(p => p.id === state.activePetId) || state.pets[0] || null;
    }
  },
  actions: {
    setPets(pets: Pet[]) {
      this.pets = pets;
      if (!this.activePetId && pets.length > 0) {
        this.setActivePet(pets[0].id);
      }
    },
    setActivePet(petId: string) {
      this.activePetId = petId;
      localStorage.setItem('petspot_active_pet_id', petId);
    },
    addPet(pet: Pet) {
      this.pets.push(pet);
      this.setActivePet(pet.id);
    }
  }
});
