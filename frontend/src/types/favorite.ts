/** 즐겨찾기 응답 항목 (백엔드 FavoriteResponseDto와 1:1 매핑) */
export interface FavoriteItem {
  favoriteId: string;
  placeId: string;
  placeName: string;
  category: string;
  categoryName: string;
  address: string;
  imageUrl: string | null;
  /** 대표 이미지 공공누리 유형 (Type3/Type4는 변경금지라 자르지 않고 표시) */
  imageCopyrightType?: string | null;
  rating: number;
  reviewCount: number;
  createdAt: string;
}
