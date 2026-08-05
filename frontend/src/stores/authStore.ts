import { defineStore } from 'pinia';
import type { User } from '@/types/user';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    currentUser: null as User | null,
    token: localStorage.getItem('petspot_token') || ''
  }),
  getters: {
    isAuthenticated: (state) => !!state.token
  },
  actions: {
    setUser(user: User, token?: string) {
      this.currentUser = user;
      if (token) {
        this.token = token;
        localStorage.setItem('petspot_token', token);
      }
    },
    logout() {
      this.currentUser = null;
      this.token = '';
      localStorage.removeItem('petspot_token');
    }
  }
});
