import { defineStore } from 'pinia';
import type { User } from '@/types/user';

function loadStoredUser(): User | null {
  const raw = localStorage.getItem('user');
  if (!raw) return null;
  try {
    return JSON.parse(raw) as User;
  } catch {
    localStorage.removeItem('user');
    return null;
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    currentUser: loadStoredUser(),
    token: localStorage.getItem('accessToken') || localStorage.getItem('petspot_token') || '',
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    userNickname: (state) => state.currentUser?.nickname || '사용자',
  },
  actions: {
    setAuth(token: string, user?: User) {
      this.token = token;
      localStorage.setItem('accessToken', token);
      if (user) {
        this.currentUser = user;
        localStorage.setItem('user', JSON.stringify(user));
      }
    },
    setUser(user: User, token?: string) {
      this.currentUser = user;
      localStorage.setItem('user', JSON.stringify(user));
      if (token) {
        this.token = token;
        localStorage.setItem('accessToken', token);
      }
    },
    logout() {
      this.currentUser = null;
      this.token = '';
      localStorage.removeItem('accessToken');
      localStorage.removeItem('petspot_token');
      localStorage.removeItem('user');
    },
  },
});
