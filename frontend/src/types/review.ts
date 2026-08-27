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
  imageUrl: string | null;
  createdAt: string;
}

/** 백엔드 `ReviewCreateRequestDto`(`POST /reviews`)와 1:1로 매핑되는 요청 타입 */
export interface ReviewCreateRequest {
  placeId: string;
  rating: number;
  content: string;
  imageUrl?: string;
}
