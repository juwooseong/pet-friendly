import { describe, it, expect, beforeEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useAuthStore } from './authStore';

describe('authStore - requiresPasswordChange', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
  });

  it('setAuth(requiresPasswordChange=true) 호출 시 상태와 localStorage에 반영된다', () => {
    const store = useAuthStore();
    store.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'nick' } as any, true);

    expect(store.requiresPasswordChange).toBe(true);
    expect(localStorage.getItem('requiresPasswordChange')).toBe('true');
  });

  it('setAuth(requiresPasswordChange=false) 호출 시 false로 반영된다', () => {
    const store = useAuthStore();
    store.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'nick' } as any, false);

    expect(store.requiresPasswordChange).toBe(false);
    expect(localStorage.getItem('requiresPasswordChange')).toBe('false');
  });

  it('completePasswordChange() 호출 시 강제 변경 상태가 해제된다', () => {
    const store = useAuthStore();
    store.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'nick' } as any, true);

    store.completePasswordChange();

    expect(store.requiresPasswordChange).toBe(false);
    expect(localStorage.getItem('requiresPasswordChange')).toBe('false');
  });

  it('flagPasswordChangeRequired() 호출 시 강제 변경 상태로 전환된다', () => {
    const store = useAuthStore();
    store.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'nick' } as any, false);

    store.flagPasswordChangeRequired();

    expect(store.requiresPasswordChange).toBe(true);
    expect(localStorage.getItem('requiresPasswordChange')).toBe('true');
  });

  it('새로고침(스토어 재생성) 후에도 requiresPasswordChange가 유지된다', () => {
    const store = useAuthStore();
    store.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'nick' } as any, true);

    // 새로고침을 흉내내기 위해 Pinia를 재생성해 스토어 state 초기화 로직(localStorage 재조회)을 다시 태운다.
    setActivePinia(createPinia());
    const reloadedStore = useAuthStore();

    expect(reloadedStore.requiresPasswordChange).toBe(true);
  });

  it('logout() 호출 시 requiresPasswordChange와 관련 localStorage가 모두 제거된다', () => {
    const store = useAuthStore();
    store.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'nick' } as any, true);

    store.logout();

    expect(store.requiresPasswordChange).toBe(false);
    expect(localStorage.getItem('requiresPasswordChange')).toBeNull();
    expect(store.isAuthenticated).toBe(false);
  });

  it('비밀번호 값 자체를 담는 상태 필드가 존재하지 않는다 (id/email/nickname/token/requiresPasswordChange만 보유)', () => {
    const store = useAuthStore();
    store.setAuth('token-abc', { id: '1', email: 'a@a.com', nickname: 'nick' } as any, true);

    expect(Object.keys(store.$state).sort()).toEqual(['currentUser', 'requiresPasswordChange', 'token'].sort());
  });
});
