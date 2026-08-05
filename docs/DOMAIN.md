# Domain Model & Business Logic (DOMAIN.md)

## 1. 도메인 엔티티 & TypeScript 타입 명세 (TypeScript Interfaces)

### 1.1 User & Pet Interfaces (`@/types/user.ts`)
```typescript
export type Species = 'DOG' | 'CAT';
export type SizeCategory = 'SMALL' | 'MEDIUM' | 'LARGE';

export interface Pet {
  id: string;
  userId: string;
  name: string;
  species: Species;
  breed: string;
  weightKg: number;
  sizeCategory: SizeCategory; // <=10kg: SMALL, <=20kg: MEDIUM, >20kg: LARGE
  ageYears: number;
  isVaccinated: boolean;
  photoUrl?: string;
  notes?: string;
  createdAt: string;
}

export interface User {
  id: string;
  email: string;
  nickname: string;
  avatarUrl?: string;
  pets: Pet[];
  activePetId?: string;
  createdAt: string;
}
```

### 1.2 Place & Pet Policy Interfaces (`@/types/place.ts`)
```typescript
export type PlaceCategory = 'CAFE' | 'HOTEL' | 'PARK' | 'HOSPITAL' | 'SALON';

export interface PetPolicy {
  maxWeightLimitKg: number | null; // null이면 제한 없음
  allowedSizes: SizeCategory[];    // ['SMALL', 'MEDIUM', 'LARGE']
  isIndoorAllowed: boolean;
  isOutdoorAllowed: boolean;
  hasOffLeashZone: boolean;
  policyChecklist: string[];       // ["예방접종 필수", "리드줄 2m 이내"]
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
```

---

## 2. 스마트 펫 매칭 알고리즘 (Smart Pet-Matching Logic)

```typescript
export function evaluatePetMatch(pet: Pet | null, place: Place): MatchResult {
  if (!pet) {
    return {
      status: 'INFO',
      message: '💡 펫 프로필을 등록하면 동반 적합도를 자동으로 계산해 드립니다.'
    };
  }

  const { petPolicy } = place;

  // 1. 체중 제한 검증
  if (petPolicy.maxWeightLimitKg && pet.weightKg > petPolicy.maxWeightLimitKg) {
    return {
      status: 'DENY',
      message: `⛔ ${pet.name}(${pet.weightKg}kg)는 장소 체중 제한(${petPolicy.maxWeightLimitKg}kg)을 초과하여 입장 불가능합니다.`
    };
  }

  // 2. 허용 견종 크기 카테고리 검증
  if (!petPolicy.allowedSizes.includes(pet.sizeCategory)) {
    const sizeLabel = pet.sizeCategory === 'LARGE' ? '대형견' : (pet.sizeCategory === 'MEDIUM' ? '중형견' : '소형견');
    return {
      status: 'WARN',
      message: `⚠️ ${pet.name}(${sizeLabel})는 매장 동반 가능 규정을 재확인해 주세요.`
    };
  }

  // 3. 예방접종 필수 지참 여부 체크
  if (petPolicy.policyChecklist.includes('VACCINE_REQUIRED') && !pet.isVaccinated) {
    return {
      status: 'WARN',
      message: `⚠️ ${pet.name}의 접종 확인서 제출이 필요한 장소입니다.`
    };
  }

  // 4. 모든 조건 통과
  return {
    status: 'PASS',
    message: `🎉 ${pet.name}(${pet.weightKg}kg) 입장 가능 및 동반 적합 장소입니다!`
  };
}
```
