import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, RouterLinkStub, type VueWrapper } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import MainLayout from './MainLayout.vue';

let currentPath = '/';

vi.mock('vue-router', () => ({
  useRoute: () => ({ path: currentPath }),
  useRouter: () => ({ push: vi.fn() }),
}));

function bottomTabs() {
  const wrapper = mount(MainLayout, { global: { stubs: { RouterLink: RouterLinkStub } } });
  return wrapper.find('nav[aria-label="하단 메뉴"]').findAllComponents(RouterLinkStub) as VueWrapper[];
}

describe('MainLayout 하단 탭 (TSK-FE-023)', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
  });

  it('휴대폰 하단 탭으로 주요 화면에 갈 수 있고, 마이페이지 하위 화면(내 반려동물)에서는 마이 탭이 활성화된다', () => {
    currentPath = '/pets';
    const tabs = bottomTabs();

    expect(tabs.map((t) => (t.props() as Record<string, unknown>).to)).toEqual(['/', '/search', '/map', '/favorites', '/mypage']);
    const active = tabs.filter((t) => t.attributes('aria-current') === 'page');
    expect(active).toHaveLength(1);
    expect(active[0].text()).toContain('마이');
  });
});
