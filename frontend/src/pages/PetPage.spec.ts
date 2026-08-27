import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import PetPage from './PetPage.vue';
import { useAuthStore } from '@/stores/authStore';

const pushMock = vi.fn();

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('@/api/apiClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn(), patch: vi.fn() },
  extractErrorMessage: (err: any, fallback: string) => err?.response?.data?.error || fallback,
}));

import apiClient from '@/api/apiClient';

function mountPage() {
  return mount(PetPage, {
    global: {
      stubs: { RouterLink: true, Teleport: true },
    },
  });
}

function loginAuthStore() {
  const authStore = useAuthStore();
  authStore.setAuth('token-abc', { id: 'u1', email: 'a@a.com', nickname: '닉네임', pets: [] });
  return authStore;
}

function makePet(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 'pet-1',
    name: '초코',
    breed: '골든리트리버',
    gender: 'MALE',
    birthDate: '2022-05-01',
    weightKg: 25.0,
    sizeCategory: 'LARGE',
    neutered: true,
    representative: true,
    photoUrl: null,
    createdAt: '2026-08-14T00:00:00',
    ...overrides,
  };
}

function mockPetsResponse(pets: Array<Record<string, unknown>> = []) {
  return {
    data: { success: true, data: pets, error: null, timestamp: '2026-08-14T00:00:00' },
  };
}

describe('PetPage', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    pushMock.mockClear();
    (apiClient.get as any).mockReset();
    (apiClient.post as any).mockReset();
    (apiClient.put as any).mockReset();
    (apiClient.delete as any).mockReset();
    (apiClient.patch as any).mockReset();
  });

  it('비로그인 사용자는 API를 호출하지 않고 로그인 화면으로 이동한다', async () => {
    mountPage();
    await flushPromises();

    expect(apiClient.get).not.toHaveBeenCalled();
    expect(pushMock).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/pets' } });
  });

  it('로그인 사용자는 GET /pets로 반려동물 목록을 조회해 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockPetsResponse([makePet()]));

    const wrapper = mountPage();
    await flushPromises();

    expect(apiClient.get).toHaveBeenCalledWith('/pets');
    expect(wrapper.text()).toContain('초코');
    expect(wrapper.text()).toContain('골든리트리버');
    expect(wrapper.text()).toContain('대형견');
    expect(wrapper.text()).toContain('대표');
  });

  it('반려동물이 없으면 EmptyState 형태의 안내를 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockPetsResponse([]));

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('등록된 반려동물이 없습니다.');
  });

  it('목록 조회 API 오류 시 기존 에러 처리 패턴으로 오류 메시지를 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockRejectedValue({
      response: { status: 500, data: { error: '서버 오류가 발생했습니다.' } },
    });

    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('서버 오류가 발생했습니다.');
  });

  it('등록 폼에서 필수값 미입력 시 API를 호출하지 않고 검증 메시지를 표시한다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockPetsResponse([]));

    const wrapper = mountPage();
    await flushPromises();

    await wrapper.find('button').trigger('click'); // + 반려동물 등록
    await flushPromises();

    const submitButton = wrapper.findAll('button').find((b) => b.text() === '등록')!;
    await submitButton.trigger('click');
    await flushPromises();

    expect(apiClient.post).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('이름, 품종, 성별, 체중(0보다 큰 값)을 모두 입력해주세요.');
  });

  it('등록 성공 시 POST /pets 호출 후 목록이 갱신된다', async () => {
    loginAuthStore();
    let getCallCount = 0;
    (apiClient.get as any).mockImplementation(() => {
      getCallCount += 1;
      if (getCallCount === 1) return Promise.resolve(mockPetsResponse([]));
      return Promise.resolve(mockPetsResponse([makePet()]));
    });
    (apiClient.post as any).mockResolvedValue({ data: { success: true, data: makePet() } });

    const wrapper = mountPage();
    await flushPromises();

    await wrapper.find('button').trigger('click'); // + 반려동물 등록
    await flushPromises();

    await wrapper.find('input[placeholder="예: 초코"]').setValue('초코');
    await wrapper.find('input[placeholder="예: 골든리트리버"]').setValue('골든리트리버');
    await wrapper.find('input[placeholder="예: 5.5"]').setValue('25');

    const submitButton = wrapper.findAll('button').find((b) => b.text() === '등록')!;
    await submitButton.trigger('click');
    await flushPromises();

    expect(apiClient.post).toHaveBeenCalledWith('/pets', {
      name: '초코',
      breed: '골든리트리버',
      gender: 'MALE',
      birthDate: null,
      weightKg: 25,
      neutered: false,
      photoUrl: null,
    });
    expect(apiClient.get).toHaveBeenCalledTimes(2);
    expect(wrapper.text()).toContain('초코');
  });

  it('수정 성공 시 PUT /pets/{petId} 호출 후 목록이 갱신된다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockPetsResponse([makePet()]));
    (apiClient.put as any).mockResolvedValue({ data: { success: true, data: makePet({ name: '초코2' }) } });

    const wrapper = mountPage();
    await flushPromises();

    const editButton = wrapper.findAll('button').find((b) => b.text() === '수정')!;
    await editButton.trigger('click');
    await flushPromises();

    const nameInput = wrapper.find('input[placeholder="예: 초코"]');
    await nameInput.setValue('초코2');

    const submitButton = wrapper.findAll('button').find((b) => b.text() === '수정 완료')!;
    await submitButton.trigger('click');
    await flushPromises();

    expect(apiClient.put).toHaveBeenCalledWith('/pets/pet-1', expect.objectContaining({ name: '초코2' }));
    expect(apiClient.get).toHaveBeenCalledTimes(2);
  });

  it('삭제 확인 후 DELETE /pets/{petId} 호출 후 목록에서 즉시 제거된다', async () => {
    loginAuthStore();
    let getCallCount = 0;
    (apiClient.get as any).mockImplementation(() => {
      getCallCount += 1;
      if (getCallCount === 1) return Promise.resolve(mockPetsResponse([makePet()]));
      return Promise.resolve(mockPetsResponse([]));
    });
    (apiClient.delete as any).mockResolvedValue({ data: { success: true, data: null } });

    const wrapper = mountPage();
    await flushPromises();

    const deleteButton = wrapper.findAll('button').find((b) => b.text() === '삭제')!;
    await deleteButton.trigger('click');
    await flushPromises();

    const confirmButtons = wrapper.findAll('button').filter((b) => b.text() === '삭제');
    await confirmButtons[confirmButtons.length - 1].trigger('click');
    await flushPromises();

    expect(apiClient.delete).toHaveBeenCalledWith('/pets/pet-1');
    expect(wrapper.text()).toContain('등록된 반려동물이 없습니다.');
  });

  it('대표 반려동물 변경 시 PATCH /pets/{petId}/representative 호출 후 목록이 갱신된다', async () => {
    loginAuthStore();
    const pet1 = makePet({ id: 'pet-1', name: '초코', representative: true });
    const pet2 = makePet({ id: 'pet-2', name: '쿠키', representative: false });
    let getCallCount = 0;
    (apiClient.get as any).mockImplementation(() => {
      getCallCount += 1;
      if (getCallCount === 1) return Promise.resolve(mockPetsResponse([pet1, pet2]));
      return Promise.resolve(
        mockPetsResponse([{ ...pet1, representative: false }, { ...pet2, representative: true }])
      );
    });
    (apiClient.patch as any).mockResolvedValue({ data: { success: true, data: { ...pet2, representative: true } } });

    const wrapper = mountPage();
    await flushPromises();

    const setRepButton = wrapper.findAll('button').find((b) => b.text() === '대표로 설정')!;
    await setRepButton.trigger('click');
    await flushPromises();

    expect(apiClient.patch).toHaveBeenCalledWith('/pets/pet-2/representative');
    expect(apiClient.get).toHaveBeenCalledTimes(2);
  });

  it('대표 반려동물 변경 API 실패 시 오류 메시지를 표시한다', async () => {
    loginAuthStore();
    const pet1 = makePet({ id: 'pet-1', name: '초코', representative: true });
    const pet2 = makePet({ id: 'pet-2', name: '쿠키', representative: false });
    (apiClient.get as any).mockResolvedValue(mockPetsResponse([pet1, pet2]));
    (apiClient.patch as any).mockRejectedValue({
      response: { status: 500, data: { error: '대표 반려동물 변경 실패' } },
    });

    const wrapper = mountPage();
    await flushPromises();

    const setRepButton = wrapper.findAll('button').find((b) => b.text() === '대표로 설정')!;
    await setRepButton.trigger('click');
    await flushPromises();

    expect(wrapper.text()).toContain('대표 반려동물 변경 실패');
  });

  it('삭제 요청이 진행 중일 때는 버튼이 비활성화되어 중복 요청이 발생하지 않는다', async () => {
    loginAuthStore();
    (apiClient.get as any).mockResolvedValue(mockPetsResponse([makePet()]));
    let resolveDelete: (value: any) => void = () => {};
    (apiClient.delete as any).mockReturnValue(
      new Promise((resolve) => {
        resolveDelete = resolve;
      })
    );

    const wrapper = mountPage();
    await flushPromises();

    const deleteButton = wrapper.findAll('button').find((b) => b.text() === '삭제')!;
    await deleteButton.trigger('click');
    await flushPromises();

    const confirmButton = wrapper.findAll('button').filter((b) => b.text() === '삭제').pop()!;
    await confirmButton.trigger('click');
    await confirmButton.trigger('click');
    await confirmButton.trigger('click');

    expect(apiClient.delete).toHaveBeenCalledTimes(1);

    resolveDelete({ data: { success: true, data: null } });
    await flushPromises();
  });
});
