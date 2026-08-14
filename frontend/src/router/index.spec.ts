import { describe, it, expect, beforeEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import router from './index';
import { useAuthStore } from '@/stores/authStore';

describe('router guard - requiresPasswordChange', () => {
  beforeEach(async () => {
    localStorage.clear();
    setActivePinia(createPinia());
    await router.push('/');
    await router.isReady();
  });

  it('requiresPasswordChange=true 상태에서 일반 화면(/search)으로 이동을 시도하면 차단되고 현재 화면에 머문다', async () => {
    const authStore = useAuthStore();
    authStore.setAuth('token', { id: '1', email: 'a@a.com', nickname: 'n' } as any, true);

    await router.push('/search');

    expect(router.currentRoute.value.name).not.toBe('Search');
    expect(router.currentRoute.value.name).toBe('Home');
  });

  it('requiresPasswordChange=false이면 일반 화면(/search)으로 정상 이동할 수 있다', async () => {
    const authStore = useAuthStore();
    authStore.setAuth('token', { id: '1', email: 'a@a.com', nickname: 'n' } as any, false);

    await router.push('/search');

    expect(router.currentRoute.value.name).toBe('Search');
  });

  it('requiresPasswordChange=true여도 로그인 화면으로는 이동할 수 있다', async () => {
    const authStore = useAuthStore();
    authStore.setAuth('token', { id: '1', email: 'a@a.com', nickname: 'n' } as any, true);

    await router.push('/login');

    expect(router.currentRoute.value.name).toBe('Login');
  });

  it('requiresPasswordChange=true 상태에서 인증이 필요한 화면(/pets)으로도 이동이 차단된다', async () => {
    const authStore = useAuthStore();
    authStore.setAuth('token', { id: '1', email: 'a@a.com', nickname: 'n' } as any, true);

    await router.push('/pets');

    expect(router.currentRoute.value.name).not.toBe('Pets');
  });
});
