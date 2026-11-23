import { defineStore } from 'pinia';
import type { User } from '@/types/user';
import { usePetStore } from './petStore';
import { usePlaceStore } from './placeStore';
import { useFavoriteStore } from './favoriteStore';

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
    /**
     * 비밀번호 변경 성공 시 강제 변경 상태를 해제한다.
     * 서버는 비밀번호가 바뀌면 이전 토큰을 무효화하므로 응답의 새 토큰으로 바꾼다 (TSK-SEC-001).
     */
    completePasswordChange(newToken?: string) {
      if (newToken) {
        this.token = newToken;
        localStorage.setItem('accessToken', newToken);
      }
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
      // 개인 데이터가 든 스토어를 비운다 - 다른 계정으로 로그인했을 때 이전 사용자의 데이터가 보이지 않게 (TSK-FE-015)
      usePetStore().$reset();
      usePlaceStore().$reset(); // 대표 반려동물 기준 크기 필터가 들어 있음
      useFavoriteStore().$reset();
      // 예전 버전이 남긴 선택 반려동물 값 (이제 서버의 대표 반려동물을 쓴다)
      localStorage.removeItem('petspot_active_pet_id');
    },
  },
});
