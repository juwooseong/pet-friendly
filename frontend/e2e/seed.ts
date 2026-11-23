/** E2E 백엔드 주소 (playwright.config.ts의 BACKEND_PORT와 같아야 한다) */
export const E2E_BACKEND_URL = 'http://127.0.0.1:8090';

/** E2E 시드 데이터 (global-setup.ts가 넣고, 테스트가 이름으로 찾는다) */
export const SEED_PLACE = {
  id: 'e2e00000-0000-4000-8000-000000000001',
  // 공공데이터 장소가 아니어야(public_data_id 없음) 상세 화면에서 TourAPI를 호출하지 않는다
  name: 'E2E 리뷰 테스트 카페',
  category: 'CAFE',
  categoryName: '카페',
  address: '서울특별시 중구 세종대로 110',
  latitude: 37.5663,
  longitude: 126.9779,
} as const;

/** 회원가입 비밀번호 정책(영문+숫자+특수문자, 8자 이상)을 만족하는 테스트 비밀번호 */
export const E2E_PASSWORD = 'E2eTest!2026';
