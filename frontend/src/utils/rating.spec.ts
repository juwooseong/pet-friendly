import { describe, it, expect } from 'vitest';
import { formatRating, NO_REVIEW_LABEL } from './rating';

describe('formatRating', () => {
  it('리뷰가 없으면 평점 대신 안내 문구를 반환한다 (과거처럼 5.0으로 표시하지 않음)', () => {
    expect(formatRating(0, 0)).toBe(NO_REVIEW_LABEL);
    expect(formatRating(null, undefined)).toBe(NO_REVIEW_LABEL);
    expect(formatRating(5, 0)).toBe(NO_REVIEW_LABEL);
  });

  it('리뷰가 있으면 소수 첫째 자리까지 평점을 반환한다', () => {
    expect(formatRating(4.25, 3)).toBe('4.3');
    expect(formatRating(3, 1)).toBe('3.0');
  });
});
