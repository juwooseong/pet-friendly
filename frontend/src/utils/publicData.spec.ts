import { describe, it, expect } from 'vitest';
import { isHttpUrl, kakaoDirectionsUrl, koglLicenseLabel } from './publicData';

describe('publicData utils', () => {
  it('공공누리 유형 코드를 읽을 수 있는 문구로 바꾸고, 모르는 값은 "공공누리"로 표시한다', () => {
    expect(koglLicenseLabel('Type1')).toBe('공공누리 제1유형(출처표시)');
    expect(koglLicenseLabel('Type3')).toBe('공공누리 제3유형(출처표시·변경금지)');
    expect(koglLicenseLabel(null)).toBe('공공누리');
  });

  it('http/https 주소만 링크로 허용한다', () => {
    expect(isHttpUrl('https://example.com')).toBe(true);
    expect(isHttpUrl('http://example.com')).toBe(true);
    expect(isHttpUrl('javascript:alert(1)')).toBe(false);
    expect(isHttpUrl(null)).toBe(false);
  });

  it('카카오맵 길찾기 링크는 장소명을 인코딩하고 좌표를 붙인다', () => {
    expect(kakaoDirectionsUrl('와룡공원 & 숲', 37.59, 126.99)).toBe(
      'https://map.kakao.com/link/to/%EC%99%80%EB%A3%A1%EA%B3%B5%EC%9B%90%20%26%20%EC%88%B2,37.59,126.99'
    );
  });
});
