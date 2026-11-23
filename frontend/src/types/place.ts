import type { SizeCategory } from './user';

export type PlaceCategory =
  | 'CAFE'
  | 'HOTEL'
  | 'PARK'
  | 'HOSPITAL'
  | 'SALON'
  | 'SHOPPING'
  | 'CULTURE'
  | 'FESTIVAL'
  | 'COURSE'
  | 'ETC';

/** 상세 사진 한 장 (TourAPI detailImage2) */
export interface PlaceGalleryImage {
  url: string;
  thumbnailUrl: string;
  /** 공공누리 유형 (Type1 등) */
  copyrightType?: string;
}

/** 상세 부가 정보 (값이 없는 키는 생략됨) */
export interface PlaceDetailInfo {
  homepage?: string;
  parking?: string;
  infoCenter?: string;
  babyCarriage?: string;
  useFee?: string;
}

/** 반려동물 정책 해석 상세 (근거가 없는 항목은 키가 없음 - ADR-007) */
export interface PetPolicyDetail {
  accompanyScope?: 'ALL_AREAS' | 'PARTIAL_AREAS';
  maxPetCount?: number;
  leashRequired?: boolean;
  carrierRequired?: boolean;
  vaccinationRequired?: boolean;
  muzzleRequiredForDangerousDogs?: boolean;
  allowedPetsText?: string;
  requirementsText?: string;
  etcText?: string;
  facilities?: string;
  furnishedItems?: string;
  purchasableItems?: string;
  rentalItems?: string;
}

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
  /** 운영시간 안내(여러 줄 평문). 공공데이터 장소는 상세 화면을 처음 열 때 채워진다 */
  operatingHours: string | null;
  /** 장소 소개(평문, 줄바꿈 포함). 공공데이터 장소는 상세 화면을 처음 열 때 채워진다 */
  description?: string | null;
  imageUrl: string | null;
  rating: number;
  reviewCount: number;
  maxWeightLimitKg: number | null;
  allowedSizes: SizeCategory[] | null;
  /** 위도/경도 조건이 주어졌을 때만 값이 채워지는 DB 계산 거리(km) */
  distanceKm: number | null;
  /** 목록용 작은 대표 이미지 (없으면 imageUrl 사용) */
  thumbnailUrl?: string | null;
  /** 대표 이미지 공공누리 유형 */
  imageCopyrightType?: string | null;
  detailInfo?: PlaceDetailInfo | null;
  galleryImages?: PlaceGalleryImage[] | null;
  petPolicyDetail?: PetPolicyDetail | null;
  /** 실내 출입 가능 여부. true=가능 확인, false=불가 확인, null=정보 없음 */
  isIndoorAllowed?: boolean | null;
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
  /** true면 sizeCategory 허용이 확인된 장소만. 기본은 확인 장소 먼저 + 크기 정보 없는 장소 뒤에 포함 */
  sizeVerifiedOnly?: boolean;
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
