/**
 * 장소 평점 표시 문자열 (TSK-BE-032)
 *
 * 리뷰가 없는 장소는 서버에서 rating=0.0으로 내려온다. 과거에는 화면에서 평점이 0/누락이면 '5.0'으로 대체 표시해
 * 리뷰가 하나도 없는 장소가 만점처럼 보였다. 리뷰 수가 0이면 숫자 대신 안내 문구를 표시한다.
 */
export const NO_REVIEW_LABEL = '평가 없음';

export function formatRating(rating: number | null | undefined, reviewCount: number | null | undefined): string {
  if (!reviewCount || reviewCount <= 0) {
    return NO_REVIEW_LABEL;
  }
  return (rating ?? 0).toFixed(1);
}
