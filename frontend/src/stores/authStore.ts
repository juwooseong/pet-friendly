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

function loadRequiresPasswordChange(): boolean {
  return localStorage.getItem('requiresPasswordChange') === 'true';
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    currentUser: loadStoredUser(),
    token: localStorage.getItem('accessToken') || localStorage.getItem('petspot_token') || '',
    // 임시 비밀번호 로그인 등으로 강제 비밀번호 변경이 필요한 상태. 비밀번호 자체는 저장하지 않는다.
    requiresPasswordChange: loadRequiresPasswordChange(),
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    userNickname: (state) => state.currentUser?.nickname || '사용자',
  },
  actions: {
    setAuth(token: string, user?: User, requiresPasswordChange = false) {
      this.token = token;
      localStorage.setItem('accessToken', token);
      if (user) {
        this.currentUser = user;
        localStorage.setItem('user', JSON.stringify(user));
      }
      this.requiresPasswordChange = requiresPasswordChange;
      localStorage.setItem('requiresPasswordChange', String(requiresPasswordChange));
    },
    setUser(user: User, token?: string) {
      this.currentUser = user;
      localStorage.setItem('user', JSON.stringify(user));
      if (token) {
        this.token = token;
        localStorage.setItem('accessToken', token);
      }
    },
    /** 서버가 강제 비밀번호 변경이 필요하다고 응답했을 때(예: 403) 상태를 동기화한다. */
    flagPasswordChangeRequired() {
      this.requiresPasswordChange = true;
      localStorage.setItem('requiresPasswordChange', 'true');
    },
    /** 비밀번호 변경 성공 시 강제 변경 상태를 해제한다. */
    completePasswordChange() {
      this.requiresPasswordChange = false;
      localStorage.setItem('requiresPasswordChange', 'false');
    },
    logout() {
      this.currentUser = null;
      this.token = '';
      this.requiresPasswordChange = false;
      localStorage.removeItem('accessToken');
      localStorage.removeItem('petspot_token');
      localStorage.removeItem('user');
      localStorage.removeItem('requiresPasswordChange');
    },
  },
});
