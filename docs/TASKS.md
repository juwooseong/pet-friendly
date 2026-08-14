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
| **Sprint 3** | Frontend MVP | **Vue 3 + Vite Frontend 9개 화면 구축 및 백엔드 API 실서비스 100% 완전 연동** | **[IN_PROGRESS]** |
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

## Sprint 3. Frontend MVP Development (Vue 3 프론트엔드 실서비스 구축) [IN_PROGRESS]

### Feature 3.1 Frontend Architecture & Design Specification [COMPLETED]
- [x] `TSK-FE-001` [P0] Frontend System Architecture & Layout Specification Document
  - Vue 3 + Vite + TS + Pinia + Vue Router 4 + Axios + TailwindCSS v4 초기 아키텍처, 공통 Layout(Main/Auth) 및 UI 컴포넌트 7종 구축 완료 (2026-08-07).
- [x] `TSK-FE-002` [P0] Pinia Store & Axios Interceptor Architecture Setup (`authStore`, `petStore`, `placeStore`)
  - `authStore`(JWT/User 영속화), `petStore`(대표 펫), `placeStore`(검색 필터 상태) 기본 구조 및 Axios Interceptor(401 처리, 에러 메시지 추출) 구축 완료.
  - **잔여**: `favoriteStore` 미구현. 각 페이지가 store action을 거치지 않고 `apiClient`를 직접 호출 중 (store는 상태 컨테이너 역할만, 데이터 페칭 로직 미위임) — 리팩토링 필요.

### Feature 3.2 Authentication & User Screens [완료]
- [x] `TSK-FE-003` [P0] Login View (`/login`): Form Validation, JWT Token Storage, Auto-login, Logout
  - 로그인 API 연동, JWT LocalStorage 저장(자동 로그인), 에러 메시지 처리 완료. CORS 및 에러 필드 버그 수정 완료 (2026-08-13).
  - 로그인 응답에 `user` 정보가 누락되어 LocalStorage가 손상되고 앱이 크래시하던 버그 수정 (2026-08-13) — 백엔드가 이제 `UserSummaryDto`를 포함해 응답.
  - `/find-id`, `/find-password` 이동 버튼 연결 완료.
- [x] `TSK-FE-004` [P0] Signup View (`/signup`): Email Duplicate Check & User Registration Flow
  - 회원가입 API 연동, 필수값 검증, 비밀번호 확인 필드, 비밀번호 패턴(영문/숫자/특수문자) 검증, 필수값 누락 시 해당 입력란 자동 포커스, Caps Lock 감지, 비밀번호 필드 한글 입력 차단 완료 (2026-08-13).
  - 이메일/닉네임 중복 시 서버 409 응답을 해당 입력란에 인라인 표시하도록 완료.
  - 실시간 중복 확인(타이핑 중 체크)은 `TSK-FE-013`(Feature 3.7)에서 완료 (2026-08-14).

### Feature 3.6 Account Recovery & Forced Password Change [COMPLETED] (Sprint 3 확장 스코프, 2026-08-13 추가)
- [x] `TSK-BE-024` [P0] Find ID API (`POST /api/v1/auth/find-id`): 닉네임 기반 마스킹 이메일 조회, 미일치 시 404
- [x] `TSK-BE-025` [P0] Find Password API (`POST /api/v1/auth/find-password`): SecureRandom 임시 비밀번호 생성 → BCrypt 저장 → `EmailSender` 추상화를 통한 발송, 계정 존재 여부 비노출
- [x] `TSK-BE-026` [P0] Change Password API (`PATCH /api/v1/auth/password`, JWT 인증): 새 비밀번호 정책/확인 일치 검증, `passwordChangeRequired` 해제
  - `PasswordChangeRequiredInterceptor`로 강제 변경 미완료 사용자의 일반 API 접근 차단 (`/auth/**`, `/places/**` 제외)
  - Flyway `V7__add_password_change_required.sql`, 로그인 응답에 `requiresPasswordChange` 포함
- [x] `TSK-BE-027` [P1] 회원가입 닉네임 중복 검증 (`DuplicateNicknameException`, 409)
- [x] `TSK-FE-012` [P0] Find ID / Find Password Views + 강제 비밀번호 변경 UI 흐름 완결 (2026-08-14 보강 완료)
  - `/find-id`, `/find-password` 화면 및 로그인 화면 연결
  - `ForcePasswordChangeModal.vue`: 로그인 응답 `requiresPasswordChange=true` 시 전 화면 비다이얼로그 레이어 표시, 닫기/취소 버튼 없음, 기존 `validators.ts`(`isValidPassword`/`doPasswordsMatch`) 재사용, `PATCH /api/v1/auth/password` 연동
  - `authStore`에 `requiresPasswordChange` 상태 추가 및 LocalStorage 영속화(새로고침 유지), `flagPasswordChangeRequired()`/`completePasswordChange()` 액션
  - Router Navigation Guard: 강제 변경 중 `Login` 외 라우트 진입 차단(URL 직접 접근 포함)
  - `apiClient.ts`: 401(로그아웃)과 403(강제 변경 필요, `X-Password-Change-Required` 헤더로 식별) 분리 처리
  - **버그 수정**: `SecurityConfig`의 CORS `allowedMethods`에 `PATCH`가 누락되어 브라우저에서 비밀번호 변경 요청이 프리플라이트 단계에서 전부 실패하던 문제 발견 및 수정 (curl 테스트로는 발견되지 않던 브라우저 전용 이슈, 실제 Playwright E2E 검증 중 확인). `PasswordResetFlowIntegrationTest`에 `X-Password-Change-Required` 헤더 검증 assertion 추가.
  - **검증 결과**: 백엔드 `./gradlew test` 전체 통과, 프런트엔드 Vitest 21/21 통과(`authStore` 7 / `LoginPage` 2 / `ForcePasswordChangeModal` 5 / router guard 4 / `apiClient` 3), `type-check`·`build` 통과, Playwright 실브라우저 시나리오 9/9 통과(회원가입→로그인→강제변경 트리거→새로고침 유지→URL 우회 차단→약한 비밀번호/불일치 거부→정상 변경 성공→플래그 해제 후 정상 접근)
  - Vitest는 이번에 프런트엔드 표준 테스트 도구로 재도입되었으며, 이후 프런트엔드 작업에서도 계속 유지한다.
  - 커밋: `ba050db`, `936551d`, `abf94a5`, `8a49270`, `dfc49d8`

### Feature 3.7 Signup Real-time Duplicate Check [COMPLETED] (2026-08-14 추가)
- [x] `TSK-BE-028` [P1] 이메일/닉네임 실시간 중복확인 API
  - `GET /api/v1/auth/check-email?email=...`, `GET /api/v1/auth/check-nickname?nickname=...`, 공용 `AvailabilityResponseDto { available }` 응답
  - `AuthController → AuthService → UserRepository` 기존 계층 구조 그대로 유지, Repository는 기존 `existsByEmail`/`existsByNickname` 재사용(신규 쿼리 없음)
  - 회원가입과 동일한 Validation 정책 적용(`@Email`, 닉네임 2~50자), `POST /auth/register`의 최종 중복 검사는 그대로 유지 — 실시간 체크는 참고용이며 서버가 최종 방어
  - **버그 수정**: `@Validated` + `@RequestParam` 제약조건이 문서에 이미 대비되어 있던 `HandlerMethodValidationException`이 아니라 `ConstraintViolationException`을 던져 500으로 떨어지던 문제 발견 및 `GlobalExceptionHandler`에 핸들러 추가로 수정
  - 보안 트레이드오프(계정 존재 여부 노출, Rate Limiting 미적용)는 `docs/DECISIONS.md` ADR-006에 기록
- [x] `TSK-FE-013` [P1] 회원가입 화면 실시간 중복확인 UI
  - `SignupPage.vue`: 이메일/닉네임 입력 400ms debounce 후 API 호출, 형식 오류(빈 값/이메일 형식/닉네임 길이) 시 호출 안 함
  - 기존 `emailError`/`nicknameError` 재사용(중복 시), `emailAvailable`/`nicknameAvailable` 신규 상태 추가(사용 가능 시)
  - `src/utils/debounce.ts` 신규 유틸(이메일/닉네임 공용), `cancel()` 지원으로 컴포넌트 unmount/재입력 시 대기 중인 요청 정리
  - 요청 토큰 기반 Race Condition 방어(느리게 도착한 과거 응답이 최신 상태를 덮어쓰지 않도록 처리)
  - **검증 결과**: 백엔드 `./gradlew test` 전체 통과, 프런트엔드 Vitest 33/33 통과(신규 12개: `SignupPage` 9 / `debounce` 3), `type-check`·`build` 통과, Playwright 실브라우저 시나리오 7/7 통과(중복 이메일/닉네임 실시간 감지 → 사용 가능 전환 → 잘못된 형식 시 API 미호출 → 정상 가입 제출 → 신규 계정 로그인 성공까지 end-to-end 확인)

### Feature 3.3 Main Discovery & Search Screens [부분 진행]
- [x] `TSK-FE-005` [P0] Home View (`/`): Recommended Places, Popular Places, Recent Places, Active Pet Chip
  - Dashboard API(`GET /api/v1/dashboard`) 연동 완료.
- [x] `TSK-FE-006` [P0] Place Search View (`/search`): Keyword Search, Category Filter, Pet Size Condition, Distance Sort (2026-08-14 완료)
  - **Backend**: `Place` 엔티티에 기존 미사용이던 `allowed_sizes`(JSONB, V1부터 존재) 컬럼을 Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)`로 매핑, `PlaceSearchCondition.sizeCategory` 조건 추가, QueryDSL `jsonb_exists()` 기반 필터링(`PlaceRepositoryImpl`) 구현. `PlaceSearchResponseDto`에 `allowedSizes` 필드 추가(응답 계약 확장, 기존 필드 변경 없음).
  - **거리순 정렬**: 기존 Sprint 1 구조(위도/경도 조건 시 ST_Distance 오름차순 자동 정렬) 그대로 유지, Pageable과 함께 정상 동작 확인.
  - **버그 발견 및 수정**: Sprint 1에서 "완료"로 표시되었던 `ST_DWithin`/`ST_Distance` PostGIS 공간 쿼리가 실제 DB를 한 번도 거치지 않은 채(기존 테스트가 전부 Mockito Mock 기반) 테스트를 통과해왔음을 발견. 실제 실행 시 Hibernate 6 HQL 파서가 PostgreSQL 전용 `::geography` 캐스트 문법을 거부하고(`unrecognized cast target type`), `ST_DWithin(...) = true` 비교도 함수 반환 타입 미등록으로 `SemanticException`을 던지는 잠재 버그를 확인 — 위도/경도를 포함한 모든 검색 요청이 실제로는 실패했을 것으로 추정됨. `geography(...)` 함수 호출 + `cast(... as boolean)` 패턴으로 수정하고 실제 PostGIS DB를 사용하는 통합 테스트(`PlaceSearchSizeAndDistanceIntegrationTest`)로 검증 완료.
  - **Frontend**: `SearchPage.vue` 전면 재구현 — 반려동물 크기 드롭다운(로그인 시 대표 반려동물 크기 자동 기본값 적용, `GET /pets` 연동), 브라우저 Geolocation 기반 "내 위치 기준 거리순 정렬" 토글(반경 선택 포함), Pageable 기반 페이지네이션 UI. `types/api.ts`(공용 `ApiResponse`/`PageResponse`), `types/place.ts`에 실제 응답 구조와 일치하는 `PlaceSearchItem`/`PlaceSearchParams` 타입 신규 추가(기존 `Place`/`PetPolicy` 아스퍼레이셔널 타입은 미사용 상태로 잔존, 하단 백로그 참조).
  - **검증**: 백엔드 `./gradlew test` 전체 통과(신규 통합 테스트 2건 포함), 프런트엔드 Vitest 38/38 통과(신규 5건: `SearchPage`), `type-check`·`build` 통과, 실브라우저(Playwright, 임시 시드 데이터 15건) 검증 — 키워드 검색/크기 필터(전체 15건→LARGE 6건 정확히 필터링)/거리순 정렬(오름차순 정확)/페이지 이동(1→2페이지 거리 연속성 유지) 전 시나리오 확인 완료.
- [ ] `TSK-FE-007` [P0] Interactive Map View (`/map`): Kakao Map / Leaflet Integration, Current Location Marker, Detail Drawer
  - **미착수**. 라우터(`router/index.ts`)에 `/map` 경로 자체가 아직 등록되지 않음.

### Feature 3.4 Place Detail & Engagement Screens [스캐폴딩 수준]
- [ ] `TSK-FE-008` [P0] Place Detail View/Modal (`/places/{id}`): Info, Photos, Pet Policy Checklist, Review Form, Favorite Toggle
  - 기본 정보 조회(`GET /places/{id}`)만 연동. **잔여**: 사진, 반려동물 동반 수칙, 리뷰 목록/작성, 즐겨찾기 토글 전부 미구현.
- [ ] `TSK-FE-009` [P0] Favorites View (`/favorites`): Saved Place Grid & Quick Delete
  - 즐겨찾기 목록 조회만 연동. **잔여**: 삭제(Quick Delete) 기능 미구현.

### Feature 3.5 Pet & My Page Screens [스캐폴딩 수준]
- [ ] `TSK-FE-010` [P0] My Pets Management View (`/pets`): Pet Multi-CRUD, Weight Input, Representative Pet Toggle
  - 반려동물 목록 조회만 연동. **잔여**: 등록/수정/삭제 폼, 대표 반려동물 스위칭 UI 전부 미구현("+ 반려동물 등록" 버튼 동작 없음).
- [ ] `TSK-FE-011` [P0] My Page View (`/mypage`): Profile Summary, Favorites Tab, My Reviews Tab, My Pets Tab
  - `GET /users/me` 기반 프로필 요약(펫/즐겨찾기/리뷰 카운트)만 표시. **잔여**: 즐겨찾기/내 리뷰/내 반려동물 탭 UI 전부 미구현.

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

### Feature 4.3 Security Hardening (Backlog, TSK-FE-012 리뷰에서 식별)
- [ ] `TSK-SEC-001` [P1] JWT 무효화(Invalidation) 정책 도입
  - **배경**: 현재 비밀번호 변경/임시 비밀번호 발급 시점에 기존 발급된 JWT를 무효화하지 않는다. Stateless JWT 특성상 서버가 개별 토큰을 강제로 만료시킬 방법이 없어, 탈취된 토큰이 있다면 비밀번호를 변경해도 만료 시각까지는 계속 유효하다.
  - **DoD**: JWT 블랙리스트(Redis 등) 또는 토큰 버전(`tokenVersion`) 관리 방식 중 택1 설계, 비밀번호 변경/강제 초기화 시 기존 토큰 즉시 무효화 적용 및 테스트.
  - **범위 제외 확인**: TSK-FE-012(2026-08-14) 보강 작업에서는 의도적으로 범위에서 제외됨.

### Feature 4.4 구조적 개선 Backlog (TSK-FE-006 리뷰에서 식별, 2026-08-14)
> 아래 항목들은 장소 검색 기능(반려동물 크기 필터/거리순 정렬) 구현 과정에서 발견되었으나, Sprint 3 기능 완성도 우선 원칙에 따라 즉시 리팩터링하지 않고 백로그로 기록한다.

- [ ] `TSK-BE-029` [P0] 장소 상세 조회 API 부재 (`GET /api/v1/places/{id}`)
  - **배경**: `PlaceController`에는 `/search`만 존재하고 단건 조회 엔드포인트가 없다. `PlaceDetailPage.vue`는 `GET /places/{id}`를 호출하고 있어 실제로는 항상 500 에러("장소 정보를 찾을 수 없습니다" 표시)가 발생한다. TASKS.md v2.0 초기 작성 시 Sprint 1에 포함된 것으로 잘못 기재되어 있었음(문서 오류였던 것으로 추정).
  - **DoD**: `PlaceQueryService`/`PlaceController`에 단건 조회 API 추가, `PlaceNotFoundException`(이미 존재) 연동, `PlaceDetailPage.vue` 정상 렌더링 확인.
- [ ] `TSK-QA-001` [P1] Sprint 1 PostGIS 공간 쿼리 실DB 통합 테스트 커버리지 부재
  - **배경**: 이번 작업 중 `PlaceRepositoryImpl`의 `ST_DWithin`/`ST_Distance` QueryDSL 템플릿이 Hibernate 6 HQL 파서에서 파싱 오류(`unrecognized cast target type: geography`) 및 타입 불일치(`SemanticException`)로 실제로는 전혀 동작하지 않았음을 발견하고 수정했다(`geography(...)` 함수 호출 + `cast(... as boolean)` 패턴으로 교체). 기존 `PlaceRepositorySearchTest`/`PlaceQueryServiceTest`/`PlaceControllerTest`가 전부 Mockito Mock 기반이라 실제 쿼리 실행 경로를 한 번도 검증하지 못했기 때문에 Sprint 1 "완료" 이후 지금까지 발견되지 않았던 것으로 보인다.
  - **DoD**: 공간/거리 관련 QueryDSL 커스텀 쿼리(및 향후 추가되는 네이티브 함수 템플릿)에 대해 최소 1개 이상의 실제 PostGIS DB 기반 통합 테스트를 필수화하는 컨벤션 수립(예: `PlaceSearchSizeAndDistanceIntegrationTest` 패턴 재사용/확장). 다른 도메인의 유사 위험(Mock만으로 커버된 실DB 의존 로직)도 함께 점검.
- [ ] `TSK-FE-014` [P2] `types/place.ts`의 `Place`/`PetPolicy` 타입이 실제 API 응답과 불일치
  - **배경**: `Place`/`PetPolicy`는 `DOMAIN.md`에 정의된 중첩 구조(`petPolicy: { allowedSizes, isIndoorAllowed, ... }`)를 따르지만, 실제 백엔드 `PlaceSearchResponseDto`는 평탄한(flat) 구조로 응답한다. 두 타입 모두 현재 어느 컴포넌트에서도 실사용되지 않는 아스퍼레이셔널(aspirational) 타입으로 방치되어 있었다. 이번 작업에서는 기존 타입을 건드리지 않고 실제 응답과 일치하는 `PlaceSearchItem`(`types/place.ts`)을 신규로 추가해 `SearchPage.vue`에서 사용하도록 했다.
  - **DoD**: `Place`/`PetPolicy`를 실제 API 응답 구조에 맞게 재정의하거나, 장소 상세 API(TSK-BE-029) 구현 시 함께 정리하여 중복 타입을 제거.
- [ ] `TSK-FE-015` [P2] `usePlaceStore`/`usePetStore` Pinia 스토어가 데이터 페칭에 관여하지 않음
  - **배경**: TASKS.md Feature 3.1에 이미 기록된 기존 기술부채("각 페이지가 store action을 거치지 않고 apiClient를 직접 호출")가 이번에 재구현한 `SearchPage.vue`에도 동일하게 적용된다(기존 코드 패턴을 그대로 따름). `usePlaceStore.places`/`usePetStore.pets`는 여전히 채워지지 않는다.
  - **DoD**: 스토어 리팩토링을 별도 스프린트로 계획할 때 장소 검색 페이지도 함께 포함.
