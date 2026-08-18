/** 즐겨찾기 응답 항목 (백엔드 FavoriteResponseDto와 1:1 매핑) */
export interface FavoriteItem {
  favoriteId: string;
  placeId: string;
  placeName: string;
  category: string;
  categoryName: string;
  address: string;
  imageUrl: string | null;
  rating: number;
  reviewCount: number;
  createdAt: string;
}
