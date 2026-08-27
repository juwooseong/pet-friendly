import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

describe('loadKakaoMaps', () => {
  beforeEach(() => {
    vi.resetModules();
    delete (window as any).kakao;
    document.head.innerHTML = '';
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('API 키가 설정되지 않으면 즉시 reject한다', async () => {
    vi.stubEnv('VITE_KAKAO_MAP_APP_KEY', '');
    const { loadKakaoMaps } = await import('./kakaoMap');

    await expect(loadKakaoMaps()).rejects.toThrow('Kakao Maps API 키가 설정되지 않았습니다.');
  });

  it('스크립트 로드 성공 시 kakao.maps 네임스페이스를 resolve한다', async () => {
    vi.stubEnv('VITE_KAKAO_MAP_APP_KEY', 'test-key');
    const { loadKakaoMaps } = await import('./kakaoMap');

    const loadPromise = loadKakaoMaps();
    const script = document.head.querySelector('script') as HTMLScriptElement;
    expect(script).not.toBeNull();
    expect(script.src).toContain('test-key');

    const fakeMaps = { load: (cb: () => void) => cb() };
    (window as any).kakao = { maps: fakeMaps };
    script.onload?.(new Event('load'));

    await expect(loadPromise).resolves.toBe(fakeMaps);
  });

  it('스크립트 로드 실패(네트워크 오류) 시 reject한다', async () => {
    vi.stubEnv('VITE_KAKAO_MAP_APP_KEY', 'test-key');
    const { loadKakaoMaps } = await import('./kakaoMap');

    const loadPromise = loadKakaoMaps();
    const script = document.head.querySelector('script') as HTMLScriptElement;
    script.onerror?.(new Event('error'));

    await expect(loadPromise).rejects.toThrow('Kakao Maps SDK 스크립트를 불러오지 못했습니다.');
  });

  it('이미 로드되어 있으면 스크립트를 다시 삽입하지 않고 즉시 resolve한다', async () => {
    vi.stubEnv('VITE_KAKAO_MAP_APP_KEY', 'test-key');
    const fakeMaps = { load: (cb: () => void) => cb() };
    (window as any).kakao = { maps: fakeMaps };
    const { loadKakaoMaps } = await import('./kakaoMap');

    const result = await loadKakaoMaps();

    expect(result).toBe(fakeMaps);
    expect(document.head.querySelector('script')).toBeNull();
  });
});
