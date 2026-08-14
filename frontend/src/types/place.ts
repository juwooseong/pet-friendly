import type { SizeCategory } from './user';

export type PlaceCategory = 'CAFE' | 'HOTEL' | 'PARK' | 'HOSPITAL' | 'SALON';

/**
 * 장소 검색 API(`GET /places/search`)의 실제 응답 Projection 구조.
 * 백엔드 `PlaceSearchResponseDto`(QueryDSL Projection)와 1:1로 매핑되며,
 * 아래 `Place`/`PetPolicy`(중첩 정책 구조)와는 별개의 평탄한(flat) 응답 형태이다.
 */
export interface PlaceSearchItem {
  id: string;
  publicDataId?: string;
  name: string;
  category: PlaceCategory;
  categoryName: string;
  address: string;
  latitude: number;
  longitude: number;
  phone: string | null;
  operatingHours: string | null;
  imageUrl: string | null;
  rating: number;
  reviewCount: number;
  maxWeightLimitKg: number | null;
  allowedSizes: SizeCategory[] | null;
  /** 위도/경도 조건이 주어졌을 때만 값이 채워지는 DB 계산 거리(km) */
  distanceKm: number | null;
}

/** 장소 검색 요청 쿼리 파라미터 (`PlaceSearchCondition` + Pageable) */
export interface PlaceSearchParams {
  latitude?: number;
  longitude?: number;
  radiusKm?: number;
  keyword?: string;
  category?: PlaceCategory;
  maxWeight?: number;
  sizeCategory?: SizeCategory;
  page?: number;
  size?: number;
}

export interface PetPolicy {
  maxWeightLimitKg: number | null;
  allowedSizes: SizeCategory[];
  isIndoorAllowed: boolean;
  isOutdoorAllowed: boolean;
  hasOffLeashZone: boolean;
  policyChecklist: string[];
}

export interface Place {
  id: string;
  publicDataId?: string;
  name: string;
  category: PlaceCategory;
  categoryName: string;
  address: string;
  latitude: number;
  longitude: number;
  phone: string;
  operatingHours: string;
  imageUrl: string;
  description: string;
  rating: number;
  reviewCount: number;
  facilities: string[];
  petPolicy: PetPolicy;
}

export type MatchStatus = 'PASS' | 'WARN' | 'DENY' | 'INFO';

export interface MatchResult {
  status: MatchStatus;
  message: string;
}
