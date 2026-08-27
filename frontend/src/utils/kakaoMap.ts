declare global {
  interface Window {
    kakao?: any;
  }
}

let loadPromise: Promise<any> | null = null;

/**
 * Kakao Maps JavaScript SDK를 동적으로 로드하고 `kakao.maps` 네임스페이스를 반환한다.
 * - API 키(`VITE_KAKAO_MAP_APP_KEY`)가 설정되지 않은 경우 즉시 reject한다(키를 소스에 하드코딩하지 않음).
 * - 스크립트 로드 실패(네트워크 오류 등) 시에도 reject한다.
 * - 이미 로드되었거나 로드 중인 경우 기존 Promise를 재사용해 스크립트 태그를 중복 삽입하지 않는다.
 */
export function loadKakaoMaps(): Promise<any> {
  const appKey = import.meta.env.VITE_KAKAO_MAP_APP_KEY as string | undefined;
  if (!appKey) {
    return Promise.reject(
      new Error('Kakao Maps API 키가 설정되지 않았습니다. 환경변수 VITE_KAKAO_MAP_APP_KEY를 확인해주세요.')
    );
  }

  if (window.kakao?.maps) {
    return Promise.resolve(window.kakao.maps);
  }

  if (loadPromise) {
    return loadPromise;
  }

  loadPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&autoload=false`;
    script.async = true;
    script.onload = () => {
      if (!window.kakao?.maps) {
        loadPromise = null;
        reject(new Error('Kakao Maps SDK 로드에 실패했습니다.'));
        return;
      }
      window.kakao.maps.load(() => resolve(window.kakao!.maps));
    };
    script.onerror = () => {
      loadPromise = null;
      reject(new Error('Kakao Maps SDK 스크립트를 불러오지 못했습니다. 네트워크 상태를 확인해주세요.'));
    };
    document.head.appendChild(script);
  });

  return loadPromise;
}

export {};
