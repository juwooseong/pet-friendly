import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import HomePage from './HomePage.vue';

vi.mock('@/api/apiClient', () => ({
  default: { get: vi.fn().mockResolvedValue({ data: { success: true, data: { content: [] } } }) },
  extractErrorMessage: (_err: any, fallback: string) => fallback,
}));

function mountPage() {
  return mount(HomePage, {
    global: { stubs: { RouterLink: { props: ['to'], template: '<a :data-to="JSON.stringify(to)"><slot /></a>' } } },
  });
}

describe('HomePage (TSK-FE-028)', () => {
  beforeEach(() => localStorage.clear());

  it('짧은 스토리는 닫으면 다음 방문에도 보이지 않는다', async () => {
    const first = mountPage();
    await flushPromises();
    expect(first.find('[data-testid="home-story"]').exists()).toBe(true);

    await first.find('button[aria-label="스토리 닫기"]').trigger('click');
    expect(first.find('[data-testid="home-story"]').exists()).toBe(false);
    first.unmount();

    const second = mountPage();
    await flushPromises();
    expect(second.find('[data-testid="home-story"]').exists()).toBe(false);
  });

  it('장소 유형 바로가기는 검색 화면을 해당 유형으로 연다', async () => {
    const wrapper = mountPage();
    await flushPromises();
    const cafe = wrapper.findAll('nav[aria-label="장소 유형 바로가기"] a').find((a) => a.text() === '음식점·카페');
    expect(JSON.parse(cafe!.attributes('data-to')!)).toEqual({ path: '/search', query: { category: 'CAFE' } });
  });
});
