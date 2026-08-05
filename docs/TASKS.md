# Task Breakdown & Implementation Roadmap (TASKS.md)

이 문서는 `SYSTEM_DESIGN.md` 및 `docs/ROADMAP.md`를 바탕으로 **Spring Boot 3 + Vue 3 (TypeScript) + PostgreSQL/PostGIS** 기반 **PetSpot** 프로젝트를 2시간 이내 실행 가능한 작업(Task) 단위로 분해한 마스터 작업 명세서입니다.

---

## 📌 Task 분해 구조 규칙
- **구조**: `Epic` ➔ `Feature` ➔ `Task`
- **단위**: 최대 2시간 내 완료 가능한 단위 (1h ~ 2h)
- **우선순위**:
  - `P0`: 블로커 및 핵심 필수 기능 (Core/Critical)
  - `P1`: 주요 기능 및 UX 향상 (High Priority)
  - `P2`: 부가 기능 및 폴리싱 (Nice to Have)

---

## 🗺️ Task Summary Dashboard (v1.1)

| Epic | Total Tasks | Backend Tasks | Frontend Tasks | QA/Infra Tasks |
| :--- | :---: | :---: | :---: | :---: |
| **Epic 1. Infra, DB & Scaffold** | 9 | 3 | 4 | 2 |
| **Epic 2. Public Data & Place Search** | 10 | 6 | 4 | 0 |
| **Epic 3. User Auth & Pet Profile** | 7 | 3 | 4 | 0 |
| **Epic 4. Smart Pet-Matching Engine** | 5 | 2 | 3 | 0 |
| **Epic 5. Social, Mobile UI & QA** | 7 | 2 | 3 | 2 |
| **합계** | **38 Tasks** | **16 Tasks** | **18 Tasks** | **4 Tasks** |

---

## Epic 1. Infra, DB & Project Scaffold (인프라 & 프로젝트 기반) [COMPLETED]

### Feature 1.1 Database & PostGIS Setup
#### `TSK-INF-001` [P0] PostgreSQL 15 + PostGIS Docker/Local Setup
- **Status**: [x] COMPLETED
- **도메인**: Infra / Database | **소요 시간**: 1h | **선행 작업**: None
- **Definition of Done (DoD)**: PostgreSQL 15 컨테이너에 PostGIS 확장(`CREATE EXTENSION postgis;`) 적용 완료.

#### `TSK-INF-002` [P0] Flyway DB Migration Scripts
- **Status**: [x] COMPLETED
- **도메인**: Infra / Database | **소요 시간**: 1.5h | **선행 작업**: `TSK-INF-001`
- **Definition of Done (DoD)**: `V1__init_schema.sql` 스크립트로 `users`, `pets`, `places`, `place_reviews` 테이블 및 Spatial Index(`idx_places_location`) 생성 완료.

---

### Feature 1.2 Spring Boot 3 Backend Scaffold
#### `TSK-BE-001` [P0] Spring Boot 3.2 Project Scaffold
- **Status**: [x] COMPLETED
- **도메인**: Backend | **소요 시간**: 1h | **선행 작업**: None
- **Definition of Done (DoD)**: Spring Boot 3.2 Gradle 프로젝트 생성, JPA, QueryDSL, Hibernate Spatial, Security 의존성 설정 완료.

#### `TSK-BE-002` [P0] SecurityConfig & JWT Provider Implementation
- **Status**: [x] COMPLETED
- **도메인**: Backend | **소요 시간**: 2h | **선행 작업**: `TSK-BE-001`
- **Definition of Done (DoD)**: JWT 토큰 생성/검증 `JwtTokenProvider` 및 `SecurityConfig` 무상태(Stateless) 세션 설정 완료.

#### `TSK-BE-003` [P0] Global Exception Handler & Standard ApiResponse Wrapper
- **Status**: [x] COMPLETED
- **도메인**: Backend | **소요 시간**: 1h | **선행 작업**: `TSK-BE-001`
- **Definition of Done (DoD)**: `@RestControllerAdvice` 기반 예외 처리기 및 `{ "success": true, "data": ... }` ApiResponse DTO 작성 완료.

---

### Feature 1.3 Vue 3 + Vite + TypeScript Frontend Scaffold
#### `TSK-FE-001` [P0] Vue 3 + Vite + TypeScript Project Scaffold
- **Status**: [x] COMPLETED
- **도메인**: Frontend | **소요 시간**: 1h | **선행 작업**: None
- **Definition of Done (DoD)**: Vue 3 + TypeScript 프로젝트 생성 및 `vue-tsc` 타입 검사 설정 완료.

#### `TSK-FE-002` [P0] TypeScript Core Interfaces Setup
- **Status**: [x] COMPLETED
- **도메인**: Frontend | **소요 시간**: 1h | **선행 작업**: `TSK-FE-001`
- **Definition of Done (DoD)**: `@/types/user.ts`, `@/types/pet.ts`, `@/types/place.ts` 인터페이스 작성 완료.

#### `TSK-FE-003` [P0] Pinia & Vue Router 4 Setup
- **Status**: [x] COMPLETED
- **도메인**: Frontend | **소요 시간**: 1.5h | **선행 작업**: `TSK-FE-002`
- **Definition of Done (DoD)**: `useAuthStore`, `usePetStore`, `usePlaceStore` 전역 스토어 쉘 설정 완료.

#### `TSK-FE-004` [P1] Design System CSS Tokens & Font Setup
- **Status**: [x] COMPLETED
- **도메인**: Frontend | **소요 시간**: 1h | **선행 작업**: `TSK-FE-001`
- **Definition of Done (DoD)**: `css/variables.css` 토큰 및 Remix Icons CDN 연결 완료.

---

## Epic 2. Public Data Ingestion & Place Search (공공데이터 수집 & 장소 조회)

### Feature 2.1 Public Data Batch Ingestion Pipeline (Backend)
#### `TSK-BE-008M` [P0] Place Mock REST Controller Endpoint (v1.1 신설)
- **도메인**: Backend | **소요 시간**: 1h | **선행 작업**: `TSK-BE-003`
- **Definition of Done (DoD)**: FE 병렬 개발을 위해 `GET /api/v1/places` Mock 응답 제공 REST 컨트롤러 작성.

#### `TSK-BE-004` [P0] TourAPI Client & Public Data DTO Parser
- **도메인**: Backend | **소요 시간**: 2h | **선행 작업**: `TSK-BE-001`
- **Definition of Done (DoD)**: 한국관광공사 TourAPI 반려동물 동반 장소 REST 응답 파싱 `TourApiClient` 구현 완료.

#### `TSK-BE-005` [P0] Public Data Scheduled Ingestion Batch (`@Scheduled`)
- **도메인**: Backend | **소요 시간**: 2h | **선행 작업**: `TSK-BE-004`, `TSK-INF-002`
- **Definition of Done (DoD)**: 주기적 수집 및 `Place` 엔티티 정제, PostGIS Point 객체 변환 후 DB Upsert 로직 완성.

---

### Feature 2.2 Place Spatial Search API (Backend)
#### `TSK-BE-006` [P0] Place JPA Entity & Spatial Repository
- **도메인**: Backend | **소요 시간**: 1.5h | **선행 작업**: `TSK-INF-002`, `TSK-BE-001`
- **Definition of Done (DoD)**: `Place` JPA 엔티티 내 `Point location` 필드 및 PostGIS Spatial Mapping 구현 완료.

#### `TSK-BE-007` [P0] QueryDSL Spatial Radius & Multi-Filter Query
- **도메인**: Backend | **소요 시간**: 2h | **선행 작업**: `TSK-BE-006`
- **Definition of Done (DoD)**: 반경 거리(`ST_DWithin`), 카테고리, 견종 크기, 실내동반 QueryDSL Repository 구현.

#### `TSK-BE-008` [P0] Place Search REST Controller Endpoints
- **도메인**: Backend | **소요 시간**: 1.5h | **선행 작업**: `TSK-BE-007`
- **Definition of Done (DoD)**: `GET /api/v1/places` (다중 필터 검색) 및 `GET /api/v1/places/{id}` 엔드포인트 테스트 통과.

---

### Feature 2.3 Place Discovery & Map Components (Frontend)
#### `TSK-FE-005` [P0] Leaflet Map Component (`MapContainer.vue`)
- **도메인**: Frontend | **소요 시간**: 2h | **선행 작업**: `TSK-FE-003`
- **Definition of Done (DoD)**: OpenStreetMap + Leaflet.js 커스텀 아이콘 마커 생성 및 Click/FlyTo 이벤트 구현 완료.

#### `TSK-FE-006` [P0] Search Box & Category Filter Pills (`FilterBar.vue`)
- **도메인**: Frontend | **소요 시간**: 1.5h | **선행 작업**: `TSK-FE-003`
- **Definition of Done (DoD)**: 키워드 검색어 입력 및 카테고리 필터 클릭 시 `usePlaceStore` 상태 업데이트.

#### `TSK-FE-007` [P1] Sub-Filter Tag Component (`SubFilterTags.vue`)
- **도메인**: Frontend | **소요 시간**: 1h | **선행 작업**: `TSK-FE-006`
- **Definition of Done (DoD)**: 소형견/중형견/대형견, 실내동반, 오프리시 존 다중 선택 태그 컴포넌트 완성.

#### `TSK-FE-008` [P0] Place Card & List Component (`PlaceCard.vue` & `PlaceList.vue`)
- **도메인**: Frontend | **소요 시간**: 2h | **선행 작업**: `TSK-FE-005`, `TSK-FE-006`
- **Definition of Done (DoD)**: 장소 카드 및 지도 마커 하이라이팅 연동.

---

## Epic 3. User Auth & Pet Profile Management (회원가입 & 펫 관리)

### Feature 3.1 User Auth API (Backend)
#### `TSK-BE-009` [P0] User Entity & Auth REST Controller
- **도메인**: Backend | **소요 시간**: 1.5h | **선행 작업**: `TSK-BE-002`
- **Definition of Done (DoD)**: 회원가입(`POST /api/v1/auth/register`), 로그인(`POST /api/v1/auth/login`) API 통과.

---

### Feature 3.2 Pet Profile CRUD API (Backend)
#### `TSK-BE-010` [P0] Pet Entity & Size Category Auto-Calculator
- **도메인**: Backend | **소요 시간**: 1.5h | **선행 작업**: `TSK-BE-009`
- **Definition of Done (DoD)**: `Pet` 엔티티 작성 및 체중에 따른 크기 카테고리 자동 산출 로직 구현.

#### `TSK-BE-011` [P0] Pet Controller REST Endpoints
- **도메인**: Backend | **소요 시간**: 1.5h | **선행 작업**: `TSK-BE-010`
- **Definition of Done (DoD)**: `POST /api/v1/pets`, `GET /api/v1/pets`, `DELETE /api/v1/pets/{id}` REST API 구현.

---

### Feature 3.3 User & Pet Profile Components (Frontend)
#### `TSK-FE-009` [P0] User Auth Modal (`AuthModal.vue`)
- **도메인**: Frontend | **소요 시간**: 1.5h | **선행 작업**: `TSK-FE-003`
- **Definition of Done (DoD)**: 회원가입 및 로그인 폼 탭 전환 및 JWT 토큰 Pinia 저장 구현.

#### `TSK-FE-010` [P0] Pet Grid & Selection Component (`PetGrid.vue`)
- **도메인**: Frontend | **소요 시간**: 1.5h | **선행 작업**: `TSK-FE-009`
- **Definition of Done (DoD)**: 등록된 펫 카드 목록 표시 및 '대표 펫으로 설정' 클릭 시 전역 상태 변경.

#### `TSK-FE-011` [P0] Add Pet Form Modal (`AddPetModal.vue`)
- **도메인**: Frontend | **소요 시간**: 1.5h | **선행 작업**: `TSK-FE-010`
- **Definition of Done (DoD)**: 이름, 견종, 체중(kg), 나이, 예방접종 여부 입력 폼 및 유효성 검사 구현.

#### `TSK-FE-012` [P0] Active Pet Header Chip (`ActivePetChip.vue`)
- **도메인**: Frontend | **소요 시간**: 1h | **선행 작업**: `TSK-FE-010`
- **Definition of Done (DoD)**: 상단 헤더에 현재 선택된 대표 펫 칩 시각화 및 클릭 시 펫 관리 모달 팝업.

---

## Epic 4. Smart Pet-Matching Engine (스마트 매칭 Engine)

### Feature 4.1 Smart Pet-Matching Evaluator (Backend)
#### `TSK-BE-012` [P0] Smart Pet-Matching Service Evaluator Logic
- **도메인**: Backend | **소요 시간**: 1.5h | **선행 작업**: `TSK-BE-006`
- **Definition of Done (DoD)**: `evaluatePetMatch(Pet, Place)` 로직 구현 및 `PASS`, `WARN`, `DENY` 반환 단위 테스트 통과.

#### `TSK-BE-013` [P0] Place Match Evaluation REST Endpoint
- **도메인**: Backend | **소요 시간**: 1h | **선행 작업**: `TSK-BE-012`, `TSK-BE-008`
- **Definition of Done (DoD)**: `POST /api/v1/places/{id}/evaluate` 엔드포인트 구현 완료.

---

### Feature 4.2 Frontend Smart Matching Engine & Badge Visualizer (Frontend)
#### `TSK-FE-013` [P0] Client-Side Match Evaluator Helper (`evaluatePetMatch.ts`)
- **도메인**: Frontend | **소요 시간**: 1h | **선행 작업**: `TSK-FE-002`
- **Definition of Done (DoD)**: 프론트엔드 전용 순수 매칭 평가 헬퍼 함수 구현 및 단위 테스트 작성.

#### `TSK-FE-014` [P0] Smart Match Badge Component (`SmartMatchTag.vue`)
- **도메인**: Frontend | **소요 시간**: 1.5h | **선행 작업**: `TSK-FE-013`, `TSK-FE-008`
- **Definition of Done (DoD)**: `PASS` (초록), `WARN` (노랑), `DENY` (빨강) 스타일 뱃지 컴포넌트 구현 및 카드 연동.

#### `TSK-FE-015` [P0] Place Detail Modal Component (`PlaceDetailModal.vue`)
- **도메인**: Frontend | **소요 시간**: 2h | **선행 작업**: `TSK-FE-014`
- **Definition of Done (DoD)**: 이미지, 주소, 전화번호, 동반 수칙 체크리스트, 매칭 뱃지 및 카카오맵 길찾기 버튼 모달 완성.

---

## Epic 5. Social, Mobile UI & QA (리뷰, 모바일 UI & 검증)

### Feature 5.1 Review & Bookmark (Backend & Frontend)
#### `TSK-BE-014` [P1] PlaceReview Entity & REST Endpoints
- **도메인**: Backend | **소요 시간**: 1.5h | **선행 작업**: `TSK-BE-008`, `TSK-BE-009`
- **Definition of Done (DoD)**: `GET /api/v1/places/{id}/reviews` 및 `POST /api/v1/places/{id}/reviews` API 구현.

#### `TSK-BE-015` [P1] Favorites / Bookmark REST Endpoints (v1.1 신설)
- **도메인**: Backend | **소요 시간**: 1h | **선행 작업**: `TSK-BE-008`, `TSK-BE-009`
- **Definition of Done (DoD)**: `POST/DELETE /api/v1/bookmarks/{placeId}` 즐겨찾기 CRUD 엔드포인트 구현.

#### `TSK-FE-016` [P1] Favorites / Bookmark Pinia Store & Heart Button
- **도메인**: Frontend | **소요 시간**: 1h | **선행 작업**: `TSK-FE-008`
- **Definition of Done (DoD)**: 카드 내 하트 클릭 시 즐겨찾기 저장/해제 및 LocalStorage/Store 보존.

#### `TSK-FE-017` [P1] Place Review Form & List Component (`ReviewSection.vue`)
- **도메인**: Frontend | **소요 시간**: 1.5h | **선행 작업**: `TSK-FE-015`
- **Definition of Done (DoD)**: 상세 모달 내 별점 선택, 리뷰 텍스트 등록 폼 및 작성된 리뷰 리스트 시각화.

---

### Feature 5.2 Mobile Responsive Layout & Polish (Frontend)
#### `TSK-FE-018` [P0] Mobile Bottom Navigation Bar & Drawer Sheet (`AppLayout.vue`)
- **도메인**: Frontend | **소요 시간**: 2h | **선행 작업**: `TSK-FE-004`, `TSK-FE-005`
- **Definition of Done (DoD)**: 900px 이하 모바일 Viewport 시 하단 탭 바 및 뷰 전환 적용.

#### `TSK-FE-019` [P2] Dark Mode Theme Switcher (`ThemeToggle.vue`)
- **도메인**: Frontend | **소요 시간**: 1h | **선행 작업**: `TSK-FE-004`
- **Definition of Done (DoD)**: `data-theme="dark"` 토글 및 다크모드 색상 변수 적용.

---

### Feature 5.3 QA & Performance Verification (QA / Test)
#### `TSK-QA-001` [P0] PostGIS Spatial Radius Query Latency Test
- **도메인**: QA / Performance | **소요 시간**: 1.5h | **선행 작업**: `TSK-BE-007`
- **Definition of Done (DoD)**: 위치 기반 반경 쿼리 응답 속도가 **P95 < 50ms** 조건 충족 보고서 작성.

#### `TSK-QA-002` [P0] Smart Pet-Matching Scenario E2E Test
- **도메인**: QA / Testing | **소요 시간**: 1.5h | **선행 작업**: `TSK-FE-014`, `TSK-BE-012`
- **Definition of Done (DoD)**: 소형견(초코 4.2kg) ➔ 대형견(빅터 28.5kg) 대표 펫 전환 시 카드 뱃지가 `PASS` ➔ `DENY`로 즉시 바뀌는지 E2E 검증 완료.
