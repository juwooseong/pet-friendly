/**
 * 공공데이터(한국관광공사 TourAPI) 표시용 유틸.
 */

/** 화면에 표시하는 출처 문구. TourAPI 데이터·사진은 공공누리 조건(출처 표시 등)으로 제공된다. */
export const TOUR_API_SOURCE = '한국관광공사';

/**
 * 공공누리 유형 코드(TourAPI cpyrhtDivCd)를 사람이 읽을 수 있는 문구로 바꾼다.
 * Type1: 출처표시 / Type2: 출처표시+상업적 이용금지 / Type3: 출처표시+변경금지 / Type4: 출처표시+상업적 이용금지+변경금지
 */
export function koglLicenseLabel(type: string | null | undefined): string {
  switch (type) {
    case 'Type1':
      return '공공누리 제1유형(출처표시)';
    case 'Type2':
      return '공공누리 제2유형(출처표시·상업적 이용금지)';
    case 'Type3':
      return '공공누리 제3유형(출처표시·변경금지)';
    case 'Type4':
      return '공공누리 제4유형(출처표시·상업적 이용금지·변경금지)';
    default:
      return '공공누리';
  }
}

/** 변경금지 조건이 붙은 공공누리 유형(제3·4유형)인지 확인한다 */
export function isNoDerivativeLicense(type: string | null | undefined): boolean {
  return type === 'Type3' || type === 'Type4';
}

/**
 * 이미지를 틀에 맞출 때 쓸 Tailwind 클래스 (TSK-FE-021, ADR-012).
 * 변경금지 유형은 잘라내지 않고 원본 비율 그대로(여백 허용) 보여 주고, 그 외에는 틀을 채우도록 자른다.
 */
export function imageFitClass(type: string | null | undefined): string {
  return isNoDerivativeLicense(type) ? 'object-contain' : 'object-cover';
}

/** http/https 주소인지 확인한다 (javascript: 등 링크로 걸면 안 되는 값 차단) */
export function isHttpUrl(value: string | null | undefined): value is string {
  return !!value && /^https?:\/\//i.test(value.trim());
}

/** 카카오맵 길찾기 링크 (목적지 이름과 좌표만 넘기며, 별도 API 키나 호출이 필요 없다) */
export function kakaoDirectionsUrl(name: string, latitude: number, longitude: number): string {
  return `https://map.kakao.com/link/to/${encodeURIComponent(name)},${latitude},${longitude}`;
}
