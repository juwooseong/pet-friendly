# [System Design & Architecture Spec] 공공데이터 기반 펫 프렌들리 서비스 (PetSpot)

Staff Software Architect 입장에서 설계한 **공공데이터 기반 펫 프렌들리 웹/모바일 서비스(PetSpot)**의 제품 기획, 데이터 모델, 시스템 아키텍처 및 설계 검증 명세 문서입니다.

---

## 1. PRD (Product Requirement Document)

### 1.1 서비스 개요 & 비전
- **서비스명**: **PetSpot (펫스팟 - 댕냥이 동반 지도)**
- **비전**: 공공데이터포털 및 한국관광공사(TourAPI)의 반려동물 동반 정보를 가공하여, 반려인이 자신의 펫(체중, 견종 크기, 예방접종 상태)에 딱 맞는 맞춤형 동반 가능 장소를 100% 안심하고 탐색할 수 있는 플랫폼.
- **핵심 차별점**:
  - 단순 장소 조회가 아닌 **"회원의 펫 프로필과 장소별 동반 수칙(Pet Policy) 간 자동 적합도 판별(Smart Pet-Matching)"** 제공.
  - 데스크톱(지도+리스트 Split View)과 모바일(하단 Navigation Bar + Drawer Sheet)을 모두 완벽 지원하는 웹/모바일 반응형 설계.
- **기술 스택 확정**: **Frontend: Vue 3 + TypeScript** (Composition API, Vite, Pinia, vue-tsc) / **Backend: Spring Boot 3** (Java 17/21, Spring Data JPA, QueryDSL, PostGIS)

---

## 2. 사용자 정의 (User Personas)

| 페르소나 | 주요 특성 | Pain Point | 해결 방안 (Solution) |
| :--- | :--- | :--- | :--- |
| **소형견 반려인** (e.g. 4kg 푸들) | 실내 카페/파스타집 선호, 멍푸치노에 관심 | 실내 동반 가능 여부와 이동가방/매너벨트 필수 정책 파악 어려움 | 실내 동반 여부 필터 및 준비물 체크리스트 뱃지 제공 |
| **대형견 반려인** (e.g. 25kg 리트리버) | 오프리시 잔디 운동장, 야외 마당 선호 | 대부분의 매장이 "10kg 이하 소형견만 가능" 정책으로 방문 거절당함 | 대형견 가능 장소 필터링 및 체중 제한 초과 시 사전 경고(DENY 뱃지) |
| **응급 진료 반려인** | 야간/휴일 급체 또는 슬개골 탈구 발생 | 24시 운영 여부, CT/MRI 보유 및 대형견 입원 가능 여부 확인 불투명 | 24시 응급 동물병원 전용 카테고리 및 연중무휴 정보 시각화 |

---

## 3. User Story & Epic

### Epic 1. 위치 기반 탐색 & 스마트 필터링
- **US-1.1**: 사용자는 카테고리(카페/식당, 숙소, 공원, 동물병원, 미용) 및 키워드/지역 검색을 통해 원하는 펫 프렌들리 장소를 탐색할 수 있다.
- **US-1.2**: 사용자는 견종 크기(소형/중형/대형견), 실내 동반, 오프리시 존 여부를 다중 선택하여 필터링할 수 있다.

### Epic 2. 회원 계정 및 펫 프로필 관리
- **US-2.1**: 사용자는 회원가입 후 내 반려동물(이름, 동물 종류, 품종, 체중, 나이, 예방접종 여부) 프로필을 다중 등록할 수 있다.
- **US-2.2**: 사용자는 등록된 펫 중 한 마리를 '대표 펫'으로 선택하거나 전환할 수 있다.

### Epic 3. 스마트 펫 매칭 & 동반 수칙 확인
- **US-3.1**: 선택된 대표 펫의 데이터(체중, 크기)와 장소의 동반 규정(최대 체중 제한, 허용 크기)이 자동 비교되어 장소 카드에 적합도 뱃지(`PASS`, `WARNING`, `DENY`)로 시각화된다.
- **US-3.2**: 사용자는 장소 상세 화면에서 공공데이터 기반 동반 체크리스트(예방접종 필수, 리드줄 길이 등) 및 카카오맵 길찾기 링크를 이용할 수 있다.

---

## 4. 기능 목록 명세 (Functional Specifications)

```
[Core Modules]
├── 1. Vue 3 + TypeScript Discovery Component & Search Engine
│   ├── Interactive Leaflet / Kakao Map Component (@types/leaflet)
│   ├── Multi-Category & Sub-filter Pipeline (Typed Pinia State Engine)
│   └── Keyword & Location Geo-search (Spring Boot Geo API)
├── 2. User & Pet Profile Management
│   ├── Spring Security + JWT Auth & Session Controller
│   ├── Pet Profile Multi-CRUD (Name, Breed, Weight, Age, Vaccine, Photo)
│   └── Active Pet Selector & Event Broadcast
├── 3. Smart Pet-Matching Engine
│   ├── Max Weight Limit Validator
│   ├── Allowed Size Category Matcher (Small, Medium, Large)
│   └── Vaccination & Policy Compliance Evaluator
└── 4. Social & User Engagement
    ├── Bookmarks / Favorites Management
    ├── Community Review & Rating System
    └── KakaoMap Nav Link & KakaoTalk Share Integration
```

---

## 5. 프론트엔드 TypeScript 인터페이스 & 데이터베이스 스키마

### 5.1 Frontend TypeScript Interfaces (`@/types/`)

```typescript
export type SizeCategory = 'SMALL' | 'MEDIUM' | 'LARGE';

export interface Pet {
  id: string;
  name: string;
  species: 'DOG' | 'CAT';
  breed: string;
  weightKg: number;
  sizeCategory: SizeCategory;
  isVaccinated: boolean;
}

export interface Place {
  id: string;
  name: string;
  category: 'CAFE' | 'HOTEL' | 'PARK' | 'HOSPITAL' | 'SALON';
  address: string;
  latitude: number;
  longitude: number;
  petPolicy: {
    maxWeightLimitKg: number | null;
    allowedSizes: SizeCategory[];
    policyChecklist: string[];
  };
}
```

### 5.2 Database Schema (PostgreSQL + PostGIS Draft)

```sql
-- 1. 회원 테이블 (Users)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. 회원의 반려동물 테이블 (Pets)
CREATE TABLE pets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    species VARCHAR(20) NOT NULL DEFAULT 'DOG',
    breed VARCHAR(100) NOT NULL,
    weight_kg NUMERIC(4,1) NOT NULL,
    size_category VARCHAR(20) NOT NULL, -- 'SMALL' (<=10kg), 'MEDIUM' (<=20kg), 'LARGE' (>20kg)
    age_years INT DEFAULT 1,
    is_vaccinated BOOLEAN DEFAULT TRUE,
    photo_url TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. 공공데이터 장소 테이블 (Places)
CREATE TABLE places (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    public_data_id VARCHAR(100) UNIQUE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL, -- 'CAFE', 'HOTEL', 'PARK', 'HOSPITAL', 'SALON'
    address TEXT NOT NULL,
    location GEOMETRY(Point, 4326), -- PostGIS 위도/경도 Spatial Point
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    phone VARCHAR(50),
    operating_hours TEXT,
    image_url TEXT,
    description TEXT,
    max_weight_limit_kg NUMERIC(4,1),
    allowed_sizes JSONB DEFAULT '["SMALL", "MEDIUM", "LARGE"]',
    is_indoor_allowed BOOLEAN DEFAULT TRUE,
    is_outdoor_allowed BOOLEAN DEFAULT TRUE,
    has_off_leash_zone BOOLEAN DEFAULT FALSE,
    policy_checklist JSONB DEFAULT '[]',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## 6. 스마트 펫 매칭 알고리즘 Specification

```java
public MatchResult evaluatePetMatch(Pet pet, Place place) {
    if (pet == null) {
        return new MatchResult("INFO", "💡 펫 프로필을 등록하면 동반 적합도를 계산해 드립니다.");
    }
    
    // 1. 체중 제한 검증
    if (place.getMaxWeightLimitKg() != null && pet.getWeightKg() > place.getMaxWeightLimitKg()) {
        return new MatchResult("DENY", 
            String.format("⛔ %s(%.1fkg)는 장소 체중 제한(%.1fkg)을 초과하여 입장 불가능합니다.", 
                pet.getName(), pet.getWeightKg(), place.getMaxWeightLimitKg()));
    }
    
    // 2. 허용 견종 크기 카테고리 검증
    if (!place.getAllowedSizes().contains(pet.getSizeCategory())) {
        return new MatchResult("WARN", 
            String.format("⚠️ %s(%s)는 동반 가능 규정을 재확인해 주세요.", pet.getName(), pet.getSizeCategory()));
    }
    
    // 3. 예방접종 필수 여부 체크
    if (place.getPolicyChecklist().contains("VACCINE_REQUIRED") && !pet.getIsVaccinated()) {
        return new MatchResult("WARN", 
            String.format("⚠️ %s의 접종 확인서 제출이 필요한 장소입니다.", pet.getName()));
    }
    
    return new MatchResult("PASS", 
        String.format("🎉 %s(%.1fkg) 입장 가능 및 동반 적합 장소입니다!", pet.getName(), pet.getWeightKg()));
}
```

---

## 7. Non-Functional Requirements (NFR) & Architecture

1. **기술 스택 확정**:
   - **Frontend**: Vue 3 + TypeScript 5.x (Vite, Pinia, Vue Router 4, Leaflet)
   - **Backend**: Spring Boot 3.2+ (Java 17/21, Spring Data JPA, QueryDSL, Spring Security + JWT, PostGIS)
2. **반응형 UX/UI 표준**:
   - 데스크톱: 1440px+ 스플릿 뷰 (리스트 460px + 지도 잔여 영역)
   - 모바일: 375px~768px 하단 네비게이션 탭 바 (60px height) + 바텀시트
3. **성능 (Performance)**:
   - Spring Boot + PostGIS 인덱싱 쿼리 (`ST_DWithin`)로 위치 기반 조회 속도 **P95 < 50ms** 달성.
4. **가용성 (Availability)**:
   - Spring Cache + Redis 및 클라이언트 파이프라인으로 외부 API 장애 시에도 서비스 99.99% 이용 보장.

---

## 8. Sprint Execution Roadmap v2.0 (실서비스 MVP 우선구축 체계)

- **Sprint 1 (Backend Core)**: PostgreSQL/PostGIS 초기화, Flyway V1~V6, TourAPI 파이프라인, 반경/필터 검색 API 완성.
- **Sprint 2 (Backend Feature Completion)**: User/Pet/Favorite/Review Core API, Review-Place rating/reviewCount 실시간 동기화, My Page API, Dashboard API, E2E Integration Test & Swagger OpenAPI 3.0 명세 완성. (기술부채는 Refactoring Sprint로 관리)
- **Sprint 3 (Frontend MVP)**: Vue 3 + Vite + TailwindCSS 기반 9개 핵심 화면 구축 및 Spring Boot 백엔드 REST API 완전 연동 MVP 서비스 완성.
- **Sprint 4 (Advanced AI & Production)**: OpenAI AI 추천, Redis Caching, Elasticsearch, AWS 배포 및 CI/CD 파이프라인 가동.

