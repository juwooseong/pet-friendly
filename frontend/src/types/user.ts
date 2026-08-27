export type SizeCategory = 'SMALL' | 'MEDIUM' | 'LARGE';
export type PetGender = 'MALE' | 'FEMALE';

/**
 * 반려동물 정보. 백엔드 `PetResponseDto`(`GET/POST/PUT /api/v1/pets`)와 1:1로 매핑된다.
 * `sizeCategory`는 항상 서버가 체중을 기준으로 계산해 내려주는 값으로, 프론트에서 별도로 판정하지 않는다.
 */
export interface Pet {
  id: string;
  name: string;
  breed: string;
  gender: PetGender;
  /** ISO 날짜 문자열(YYYY-MM-DD) 또는 미입력 시 null */
  birthDate: string | null;
  weightKg: number;
  sizeCategory: SizeCategory;
  neutered: boolean;
  /** 대표 반려동물 여부 */
  representative: boolean;
  photoUrl: string | null;
  createdAt?: string;
}

/** 반려동물 등록 요청 (`POST /api/v1/pets`, 백엔드 `PetCreateRequestDto`와 1:1 매핑) */
export interface PetCreateRequest {
  name: string;
  breed: string;
  gender: PetGender;
  birthDate?: string | null;
  weightKg: number;
  neutered?: boolean;
  photoUrl?: string | null;
}

/** 반려동물 수정 요청 (`PUT /api/v1/pets/{petId}`, 백엔드 `PetUpdateRequestDto`와 1:1 매핑) */
export type PetUpdateRequest = PetCreateRequest;

export interface User {
  id: string;
  email: string;
  nickname: string;
  avatarUrl?: string;
  pets: Pet[];
  activePetId?: string;
}
