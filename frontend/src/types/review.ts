/** 백엔드 `ReviewResponseDto`(`GET /places/{placeId}/reviews`, `POST /reviews`)와 1:1로 매핑되는 응답 타입 */
export interface Review {
  id: string;
  placeId: string;
  placeName: string;
  userId: string;
  userNickname: string;
  userAvatarUrl: string | null;
  rating: number;
  content: string;
  /** 리뷰 사진 원본 (크게 보기용, 없으면 null) */
  imageUrl: string | null;
  /** 리뷰 목록용 정사각형 썸네일 (없으면 null) */
  thumbnailUrl: string | null;
  createdAt: string;
}

/** 백엔드 `ReviewCreateRequestDto`(`POST /reviews`)와 1:1로 매핑되는 요청 타입 */
export interface ReviewCreateRequest {
  placeId: string;
  rating: number;
  content: string;
  imageUrl?: string;
}

/** `POST /review-images` 응답 - imageUrl을 리뷰 등록 요청에 넣는다 */
export interface ReviewImageUpload {
  imageUrl: string;
  thumbnailUrl: string;
}
