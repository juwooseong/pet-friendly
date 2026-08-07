# Task Breakdown & Implementation Roadmap (TASKS.md)

이 문서는 `SYSTEM_DESIGN.md` 및 `docs/ROADMAP.md`를 바탕으로 **Spring Boot 3 + Vue 3 (TypeScript) + PostgreSQL/PostGIS** 기반 **PetSpot** 프로젝트를 작업(Task) 단위로 분해한 마스터 작업 명세서 (v2.0)입니다.

---

## 📌 Task 분해 구조 규칙
- **구조**: `Epic` ➔ `Feature` ➔ `Task`
- **우선순위**:
  - `P0`: 블로커 및 핵심 필수 기능 (Core/Critical)
  - `P1`: 주요 기능 및 UX 향상 (High Priority)
  - `P2`: 부가 기능 및 폴리싱 (Nice to Have)

---

## 🗺️ Sprint Overview Dashboard (v2.0)

| Sprint | Goal | Key Scope | Status |
| :--- | :--- | :--- | :---: |
| **Sprint 0** | Infrastructure | 개발 환경 구축, DB/PostGIS 초기화 & 프로젝트 스캐폴딩 | **[COMPLETED]** |
| **Sprint 1** | Backend Core | 공공데이터 수집 파이프라인, PostGIS 반경/필터 검색 API | **[COMPLETED]** |
| **Sprint 2** | Backend Complete | **User/Pet/Favorite/Review Core API, My Page API, Dashboard API, Integration Test & Swagger** | **[IN_PROGRESS]** |
| **Sprint 3** | Frontend MVP | **Vue 3 + Vite Frontend 9개 화면 구축 및 백엔드 API 실서비스 100% 완전 연동** | **[NEXT]** |
| **Sprint 4** | Advanced AI & Infra | OpenAI AI 추천, Redis Caching, Elasticsearch, AWS CI/CD 및 모니터링 | [PLANNED] |

---

## Sprint 1. Backend Core & Public Data Pipeline [COMPLETED]

### Feature 1.1 DB & Spatial Core
- [x] `TSK-INF-001` PostgreSQL 15 + PostGIS Docker Setup
- [x] `TSK-INF-002` Flyway DB Migration (`V1` ~ `V6`)
- [x] `TSK-BE-004` TourAPI Public Data Parser & Ingestion Batch (`@Scheduled`)
- [x] `TSK-BE-006` Place JPA Entity & Spatial Repository
- [x] `TSK-BE-007` QueryDSL Spatial Radius & Multi-Filter Query (Pageable)
- [x] `TSK-BE-008` Place Search REST Controller (`GET /api/v1/places/search`, `GET /api/v1/places/{id}`)

---

## Sprint 2. Backend Feature Completion (백엔드 기능 완결) [IN_PROGRESS]

### Feature 2.1 User & Auth Domain [COMPLETED]
- [x] `TSK-BE-009` User Entity (`register` 정적 팩토리, `passwordHash`, `withdrawAt`), UserRepository & Flyway V1~V3
- [x] `TSK-BE-010` User Register API (`POST /api/v1/auth/register`) & DuplicateEmailException (409)
- [x] `TSK-BE-011` User Login API (`POST /api/v1/auth/login`), JwtTokenProvider SHA-256 & InvalidCredentialsException (401)
- [x] `TSK-BE-012` JwtAuthenticationFilter (OncePerRequestFilter), CustomUserDetails/Service & SecurityConfig 연동

### Feature 2.2 User Profile & Pet Management Domain [COMPLETED]
- [x] `TSK-BE-013` My Profile API (`GET /api/v1/users/me`, `PUT /api/v1/users/me`), UserProfileService & UserNotFoundException (404)
- [x] `TSK-BE-014` Pet Entity, PetGender, PetSizeCategory (체중별 자동 계산), PetRepository & Flyway V4
- [x] `TSK-BE-015` Pet CRUD REST API (`POST`, `GET`, `PUT`, `DELETE /api/v1/pets`), 대표 펫 삭제 승계 & Owner 권한 검증 (403)
- [x] `TSK-BE-016` Representative Pet Switch API (`PATCH /api/v1/pets/{petId}/representative`) & Atomic Transaction

### Feature 2.3 Favorites & Review Domain [COMPLETED]
- [x] `TSK-BE-017` Favorite Entity, FavoriteRepository & Flyway V5 (`favorites` N:M 해소 엔티티)
- [x] `TSK-BE-018` Review Entity (Soft Delete `deleted`), ReviewRepository & Flyway V6 (`reviews` 엔티티)
- [x] `TSK-BE-019` Review Service & REST API (`POST`, `GET`, `PUT`, `DELETE /api/v1/reviews`), Place rating & reviewCount 자동 갱신, 409 Conflict 중복 방지

### Feature 2.4 My Page & Dashboard Summary API [IN_PROGRESS]
- [ ] `TSK-BE-020` [P0] My Page Summary API (`GET /api/v1/users/me/summary`)
  - **DoD**: 사용자 정보, 대표 반려동물, 즐겨찾기 목록, 내 리뷰 목록, 내 반려동물 목록 일괄 통합 응답 DTO 및 서비스 구현.
- [ ] `TSK-BE-021` [P0] Dashboard Home API (`GET /api/v1/dashboard`)
  - **DoD**: 홈 화면용 추천 장소, 인기 장소, 최근 등록 장소, 대표 반려동물 통합 조회 REST API 구현.

### Feature 2.5 Integration Test & API Documentation [IN_PROGRESS]
- [ ] `TSK-BE-022` [P0] Full Flow End-to-End Integration Test (`BackendE2EFlowIntegrationTest`)
  - **DoD**: 회원가입 ➔ 로그인 ➔ JWT 발급 ➔ Pet 등록 ➔ Favorite 등록 ➔ Review 작성 ➔ Place Rating 변경 ➔ 마이페이지 조회 전체 파이프라인 검증 테스트 통과.
- [ ] `TSK-BE-023` [P0] Swagger / OpenAPI 3.0 Documentation Consolidation
  - **DoD**: Swagger Tag 정리, Request/Response Example 및 Error Response 명세 작성 완료.

> [!NOTE]
> **Sprint 2 종료 후 Refactoring Sprint 수용 항목 (기술부채)**:
> `ST_DWithin` 적용, Pagination 최적화, QueryDSL 개선, Domain/Application Layer 정제, Exception 개선, Service/Repository 리팩토링, Performance Optimization.

---

## Sprint 3. Frontend MVP Development (Vue 3 프론트엔드 실서비스 구축) [NEXT]

### Feature 3.1 Frontend Architecture & Design Specification
- [ ] `TSK-FE-001` [P0] Frontend System Architecture & Layout Specification Document
- [ ] `TSK-FE-002` [P0] Pinia Store & Axios Interceptor Architecture Setup (`authStore`, `petStore`, `placeStore`, `favoriteStore`)

### Feature 3.2 Authentication & User Screens
- [ ] `TSK-FE-003` [P0] Login View (`/login`): Form Validation, JWT Token Storage, Auto-login, Logout
- [ ] `TSK-FE-004` [P0] Signup View (`/signup`): Email Duplicate Check & User Registration Flow

### Feature 3.3 Main Discovery & Search Screens
- [ ] `TSK-FE-005` [P0] Home View (`/`): Recommended Places, Popular Places, Recent Places, Active Pet Chip
- [ ] `TSK-FE-006` [P0] Place Search View (`/places`): Keyword Search, Category Filter, Pet Size Condition, Distance Sort
- [ ] `TSK-FE-007` [P0] Interactive Map View (`/map`): Kakao Map / Leaflet Integration, Current Location Marker, Detail Drawer

### Feature 3.4 Place Detail & Engagement Screens
- [ ] `TSK-FE-008` [P0] Place Detail View/Modal (`/places/{id}`): Info, Photos, Pet Policy Checklist, Review Form, Favorite Toggle
- [ ] `TSK-FE-009` [P0] Favorites View (`/favorites`): Saved Place Grid & Quick Delete

### Feature 3.5 Pet & My Page Screens
- [ ] `TSK-FE-010` [P0] My Pets Management View (`/pets`): Pet Multi-CRUD, Weight Input, Representative Pet Toggle
- [ ] `TSK-FE-011` [P0] My Page View (`/mypage`): Profile Summary, Favorites Tab, My Reviews Tab, My Pets Tab

---

## Sprint 4. Advanced AI, Infra & Production (고급 기능 & 인프라) [PLANNED]

### Feature 4.1 AI Recommendation & Advanced Search
- [ ] `TSK-AI-001` OpenAI Pet-Tailored AI Place Recommendation Pipeline
- [ ] `TSK-INF-003` Redis Caching for Geo-search & JWT Tokens
- [ ] `TSK-INF-004` Elasticsearch Full-Text Search Integration

### Feature 4.2 Production Infra & Monitoring
- [ ] `TSK-INF-005` Docker Compose Production Environment Setup
- [ ] `TSK-INF-006` AWS Cloud Deployment & GitHub Actions CI/CD Pipeline
- [ ] `TSK-INF-007` Prometheus & Grafana Monitoring & APM Setup
