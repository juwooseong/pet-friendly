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
| **Sprint 2** | Backend Complete | **User/Pet/Favorite/Review Core API, My Page API, Dashboard API, Integration Test & Swagger** | **[COMPLETED]** (2026-11-19) |
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

## Sprint 2. Backend Feature Completion (백엔드 기능 완결) [COMPLETED] (2026-11-19)

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

### Feature 2.4 My Page & Dashboard Summary API [COMPLETED] (2026-08-27 문서 반영 — 실제로는 이전 스프린트에서 이미 구현되어 있었음)
- [x] `TSK-BE-020` [P0] My Page Summary API — **완료 확인 (2026-08-27)**
  - 실제 엔드포인트는 문서에 기재된 `GET /api/v1/users/me/summary`가 아니라 `GET /api/v1/users/me`(`UserController` → `MyPageQueryService`)이며, DoD에 적힌 "즐겨찾기/리뷰/반려동물 목록 일괄 포함" 대신 사용자 정보 + 대표 반려동물(`RepresentativePetDto`) + 각 카운트(`petCount`/`favoriteCount`/`reviewCount`)만 통합 응답하는 설계로 구현되어 있었다(목록 자체는 기존 `GET /pets`, `GET /users/me/favorites`, `GET /users/me/reviews`를 각 화면에서 그대로 재사용, 중복 응답 방지). `TSK-FE-011`(MyPage 연동) 작업 중 이 사실을 확인해 문서를 실제 상태로 갱신.
- [x] `TSK-BE-021` [P0] Dashboard Home API (`GET /api/v1/dashboard`) — **완료 확인 (2026-08-27)**
  - `DashboardController`/`DashboardQueryService`로 이미 구현되어 있었고, `TSK-FE-005`(홈 화면)에서 이미 연동해 사용 중이었다. Sprint 2 체크리스트에 반영되지 않은 문서 누락이었음을 확인해 갱신.

### Feature 2.5 Integration Test & API Documentation [COMPLETED] (2026-11-19)
- [x] `TSK-BE-022` [P0] Full Flow End-to-End Integration Test (`Sprint2FullFlowIntegrationTest`) — **완료 (2026-11-19, 아래 "완료 기록" 참조)**
  - **DoD**: 회원가입 ➔ 로그인 ➔ JWT 발급 ➔ Pet 등록 ➔ Favorite 등록 ➔ Review 작성 ➔ Place Rating 변경 ➔ 마이페이지 조회 전체 파이프라인 검증 테스트 통과.
  - 테스트 자체는 2026-08-07에 `Sprint2FullFlowIntegrationTest`로 이미 구현되어 있었으나(문서에는 `BackendE2EFlowIntegrationTest`로 잘못 기재되어 미완료로 남아 있었음), DoD의 "Place Rating 변경" 검증이 빠져 있어 보강 후 완료 처리.
- [x] `TSK-BE-023` [P0] Swagger / OpenAPI 3.0 Documentation Consolidation — **완료 (2026-11-19, 아래 "완료 기록" 참조)**
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
- [x] `TSK-FE-007` [P0] Interactive Map View (`/map`): Kakao Map SDK 로딩/렌더링 기반 — **완료 (2026-08-27, 0단계: SDK+렌더링만)**
  - Kakao Maps JavaScript SDK 연동 구조(`VITE_KAKAO_MAP_APP_KEY`), `/map` 라우트, 현재 위치/기본 위치(서울시청) 기준 지도 렌더링, SDK 로딩 실패 오류 표시, 언마운트 시 리소스 정리까지 완료.
  - **의도적 범위 축소(2026-08-27)**: 직전 커밋(`4a4b00c`)에서 함께 구현했던 "기존 `/places/search` 재사용 마커 표시 + 마커 클릭 시 장소 카드 + 상세 이동"은 이번 요청에 따라 되돌리고, 이번 단계는 "SDK 로딩 및 지도 렌더링 자체 검증"에만 집중하도록 축소했다. 마커/장소 연동은 다음 Task에서 별도로 재구현 예정(버그 아님, 의도된 단계적 진행).
  - 상세 내용은 하단 "2026-08-27 완료 기록: Kakao Map SDK 로딩/렌더링 기반 (0단계)" 참조.

### Feature 3.4 Place Detail & Engagement Screens [스캐폴딩 수준]
- [ ] `TSK-FE-008` [P0] Place Detail View/Modal (`/places/{id}`): Info, Photos, Pet Policy Checklist, Review Form, Favorite Toggle
  - 기본 정보 조회(`GET /places/{id}`, TSK-BE-029로 완료), **즐겨찾기 토글 연동 완료 (2026-08-18)**: 로그인 사용자 기준 즐겨찾기 상태 표시(`GET /users/me/favorites` 재사용), 추가/삭제 토글(`POST`/`DELETE /favorites/{placeId}`), 비로그인 시 로그인 화면(`redirect` 쿼리 포함) 이동, 요청 중 버튼 잠금(중복 클릭 방지), 실패 시 상태 유지 + `ErrorMessage`로 오류 표시.
  - **리뷰 목록 조회/작성 폼 연동 완료 (2026-08-18)**: 기존 `GET /places/{placeId}/reviews`/`POST /reviews` API 재사용, 리뷰 없을 시 `EmptyState` 표시, 로그인 사용자 대상 평점(1~5)/내용 입력 폼(비로그인 시 로그인 안내로 대체), 등록 성공 시 리뷰 목록과 장소 `rating`/`reviewCount` 즉시 재조회 갱신, 중복 작성(409)·검증 실패(400) 등 API 실패 시 기존 `extractErrorMessage`/`ErrorMessage` 패턴으로 오류 표시. 상세 내용은 하단 "2026-08-18 완료 기록" 참조(즐겨찾기/리뷰 각각 별도 기록).
  - **사진/반려동물 동반 수칙 보강 완료 (2026-08-27)**: 사진은 `imageUrl` 없음/로드 실패(`@error`) 시 기존 fallback 아이콘 UI로 대체. 동반 수칙은 현재 API가 실제로 제공하는 `allowedSizes`(기존 크기 배지 UI 유지)/`maxWeightLimitKg`(신규 표시, 기존에 응답에는 있었으나 화면 미표시 상태였음)만 "반려동물 동반 수칙" 섹션에 표시, 정보가 전혀 없으면 안내 문구 표시. 상세 내용은 하단 "2026-08-27 완료 기록" 참조.
  - **잔여**: 장소 사진은 여전히 `imageUrl` 단일 필드 기반(다중 사진 갤러리 아님) — 한국관광공사 이미지 데이터 연동은 Sprint 4 스코프. 실내/실외 동반 가능 여부·목줄 자유구역 등 `Place` 엔티티에는 있지만 API로 노출되지 않는 정책 필드는 `TSK-BE-031` 백로그 참조.
- [x] `TSK-FE-009` [P0] Favorites View (`/favorites`): Saved Place Grid & Quick Delete — **완료 (2026-08-27)**
  - `FavoritesPage.vue`를 기존 `GET /api/v1/users/me/favorites`(기존 API, 신규 없음) 연동으로 재작성. 장소명/주소/이미지(없음/로드 실패 fallback 아이콘)/카테고리/평점·리뷰 수를 카드로 표시, 카드 클릭 시 기존 `/place/:id`(`PlaceDetail`) 라우트로 이동. 삭제는 기존 `DELETE /api/v1/favorites/{placeId}` 재사용, 카드 클릭과 분리된 삭제 버튼(`@click.stop`), 요청 중 버튼 잠금(중복 클릭 방지), 성공 시 목록에서 즉시 제거, 실패 시 목록 유지 + `ErrorMessage`. 로딩/빈 목록/API 오류는 기존 `LoadingSpinner`/`EmptyState`/`ErrorMessage` 패턴 재사용. 비로그인 접근은 라우터 가드(`meta.requiresAuth`)로 우선 차단되지만, 컴포넌트 자체에서도 방어적으로 `authStore.isAuthenticated`를 확인해 로그인 화면(`redirect` 쿼리)으로 이동. `favoriteStore`는 이번에도 도입하지 않고 기존 프로젝트 패턴(페이지에서 `apiClient` 직접 호출)을 그대로 따름 — 기존 기술부채(`TSK-FE-015`)에 계속 해당.
  - 상세 내용은 하단 "2026-08-27 완료 기록: 즐겨찾기 목록 화면 CRUD 연동" 참조.

### Feature 3.5 Pet & My Page Screens [스캐폴딩 수준]
- [x] `TSK-FE-010` [P0] My Pets Management View (`/pets`): Pet Multi-CRUD, Weight Input, Representative Pet Toggle — **완료 (2026-08-27)**
  - `PetPage.vue`를 기존 `PetController`(`GET`/`POST`/`PUT`/`DELETE /api/v1/pets`, `PATCH /api/v1/pets/{petId}/representative`, 전부 기존 API, 신규 없음) 연동으로 전면 재작성. 목록 조회, 등록/수정(`BaseModal` 공용 폼), 삭제(확인 모달), 대표 반려동물 지정 전부 구현.
  - `sizeCategory`는 서버 응답값을 그대로 표시만 하고 프론트에서 별도 계산 로직을 두지 않음(체중 변경 시 서버가 재계산한 값을 응답으로 받아 화면에 반영).
  - 상세 내용은 하단 "2026-08-27 완료 기록: 반려동물 관리 화면 CRUD 연동" 참조.
- [x] `TSK-FE-011` [P0] My Page View (`/mypage`): Profile Summary, Favorites Tab, My Reviews Tab, My Pets Tab — **완료 (2026-08-27)**
  - `MyPage.vue`를 기존 `GET /api/v1/users/me`(`MyPageQueryService`, 신규 API 없음) 연동으로 재작성: 이메일/닉네임/프로필 이미지(있을 때만) 표시, 대표 반려동물(이름/품종/체중/크기, 없으면 안내 문구) 표시, 펫/즐겨찾기/리뷰 카운트 표시.
  - "탭"은 각 기능이 이미 별도 화면(`/favorites`, `/pets`)으로 구현되어 있어 중복 구현하지 않고 라우터 이동 버튼/카드로 연결. 리뷰는 기존에 화면이 없었으나 백엔드에 `GET /api/v1/users/me/reviews`(기존 `ReviewController`, 신규 아님) API가 이미 있었음을 확인해, 최소 범위의 읽기 전용 `MyReviewsPage.vue`(`/mypage/reviews`)를 신규로 구현해 연결(수정/삭제는 이번 범위에 없음, 필요 시 별도 백로그).
  - **회원 정보 수정**: 백엔드에 이미 존재하는 `PUT /api/v1/users/me`(`UserProfileService`, 닉네임 필수/프로필 이미지 URL 선택)를 확인 후 재사용해 닉네임/프로필 이미지 수정 모달 구현. 성공 시 `authStore.currentUser`도 함께 갱신해 상단 헤더의 닉네임 표시와 즉시 동기화.
  - 상세 내용은 하단 "2026-08-27 완료 기록: 마이페이지 통합 화면 연동" 참조.

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

- [x] `TSK-BE-029` [P0] 장소 상세 조회 API 부재 (`GET /api/v1/places/{id}`) — **완료 (2026-08-18)**
  - **배경**: `PlaceController`에는 `/search`만 존재하고 단건 조회 엔드포인트가 없다. `PlaceDetailPage.vue`는 `GET /places/{id}`를 호출하고 있어 실제로는 항상 500 에러("장소 정보를 찾을 수 없습니다" 표시)가 발생한다. TASKS.md v2.0 초기 작성 시 Sprint 1에 포함된 것으로 잘못 기재되어 있었음(문서 오류였던 것으로 추정).
  - **DoD 충족 확인**: `PlaceController.getPlaceDetail`(`GET /api/v1/places/{placeId}`) 및 `PlaceQueryService.getPlaceDetail`/`PlaceRepositoryCustom·Impl.findPlaceDetail` 추가, 미존재 ID는 기존 `PlaceNotFoundException` → 404 연동, `PlaceDetailPage.vue` 정상 렌더링 확인(`PlaceDetailIntegrationTest`, `PlaceControllerTest`, `PlaceQueryServiceTest`로 검증).
- [ ] `TSK-QA-001` [P1] Sprint 1 PostGIS 공간 쿼리 실DB 통합 테스트 커버리지 부재
  - **배경**: 이번 작업 중 `PlaceRepositoryImpl`의 `ST_DWithin`/`ST_Distance` QueryDSL 템플릿이 Hibernate 6 HQL 파서에서 파싱 오류(`unrecognized cast target type: geography`) 및 타입 불일치(`SemanticException`)로 실제로는 전혀 동작하지 않았음을 발견하고 수정했다(`geography(...)` 함수 호출 + `cast(... as boolean)` 패턴으로 교체). 기존 `PlaceRepositorySearchTest`/`PlaceQueryServiceTest`/`PlaceControllerTest`가 전부 Mockito Mock 기반이라 실제 쿼리 실행 경로를 한 번도 검증하지 못했기 때문에 Sprint 1 "완료" 이후 지금까지 발견되지 않았던 것으로 보인다.
  - **DoD**: 공간/거리 관련 QueryDSL 커스텀 쿼리(및 향후 추가되는 네이티브 함수 템플릿)에 대해 최소 1개 이상의 실제 PostGIS DB 기반 통합 테스트를 필수화하는 컨벤션 수립(예: `PlaceSearchSizeAndDistanceIntegrationTest` 패턴 재사용/확장). 다른 도메인의 유사 위험(Mock만으로 커버된 실DB 의존 로직)도 함께 점검.
- [ ] `TSK-FE-014` [P2] `types/place.ts`의 `Place`/`PetPolicy` 타입이 실제 API 응답과 불일치
  - **배경**: `Place`/`PetPolicy`는 `DOMAIN.md`에 정의된 중첩 구조(`petPolicy: { allowedSizes, isIndoorAllowed, ... }`)를 따르지만, 실제 백엔드 `PlaceSearchResponseDto`는 평탄한(flat) 구조로 응답한다. 두 타입 모두 현재 어느 컴포넌트에서도 실사용되지 않는 아스퍼레이셔널(aspirational) 타입으로 방치되어 있었다. 이번 작업에서는 기존 타입을 건드리지 않고 실제 응답과 일치하는 `PlaceSearchItem`(`types/place.ts`)을 신규로 추가해 `SearchPage.vue`에서 사용하도록 했다.
  - **DoD**: `Place`/`PetPolicy`를 실제 API 응답 구조에 맞게 재정의하거나, 장소 상세 API(TSK-BE-029) 구현 시 함께 정리하여 중복 타입을 제거.
- [ ] `TSK-FE-015` [P2] `usePlaceStore`/`usePetStore` Pinia 스토어가 데이터 페칭에 관여하지 않음
  - **배경**: TASKS.md Feature 3.1에 이미 기록된 기존 기술부채("각 페이지가 store action을 거치지 않고 apiClient를 직접 호출")가 이번에 재구현한 `SearchPage.vue`에도 동일하게 적용된다(기존 코드 패턴을 그대로 따름). `usePlaceStore.places`/`usePetStore.pets`는 여전히 채워지지 않는다. `PlaceDetailPage.vue`의 즐겨찾기 연동(2026-08-18)도 `favoriteStore` 없이 동일 패턴으로 구현되어 이 부채가 계속 누적되고 있음.
  - **DoD**: 스토어 리팩토링을 별도 스프린트로 계획할 때 장소 검색 페이지도 함께 포함.
- [ ] `TSK-BE-030` [P2] 장소 단건 즐겨찾기 여부 확인 전용 API 부재
  - **배경**: 이번 장소 상세 즐겨찾기 토글 연동(2026-08-18) 작업 중 확인한 기존 백엔드 API에는 특정 장소 1건에 대한 즐겨찾기 여부만 확인하는 엔드포인트(예: `GET /favorites/{placeId}/status`)가 없다. `PlaceDetailPage.vue`는 이를 우회해 기존 `GET /users/me/favorites`(전체 목록)를 호출한 뒤 클라이언트에서 `placeId` 일치 여부를 찾는 방식으로 구현했다(요구사항에 따라 기존 API를 최대한 재사용). 즐겨찾기 수가 많아지면 상세 페이지 진입마다 불필요하게 전체 목록을 내려받는 비효율이 있다.
  - **DoD**: 즐겨찾기 수 증가에 따른 실측 성능 영향을 확인한 뒤, 필요 시 단건 상태 확인 전용 API(또는 장소 상세 응답에 `isFavorited` 필드 포함) 추가를 검토.
- [ ] `TSK-QA-002` [P1] Playwright E2E 테스트 하네스 부재 (`TSK-FE-008` 리뷰 기능 검증에서 식별, 2026-08-18)
  - **배경**: 장소 상세 리뷰 조회/작성 기능의 실브라우저 검증을 위해 로컬에 Playwright를 임시 설치하고, DB에 임시 시드 장소/계정 데이터를 스크립트로 직접 심어 수동으로 시나리오(로그인→검색→상세→리뷰 작성→새로고침→즐겨찾기 확인)를 실행했다. 저장소에는 재사용 가능한 Playwright 설정, 테스트용 DB 초기화/시드 스크립트, `npm run e2e` 같은 표준 진입점이 없어 매번 이 과정을 수작업으로 반복해야 한다.
  - **DoD**: `playwright.config.ts` 및 `frontend`(또는 루트) `e2e/` 디렉터리 신설, 테스트 전용 DB 시드/정리 스크립트 작성, CI에서 실행 가능한 `npm run e2e` 스크립트 추가. 최소한 이번에 검증한 리뷰 작성 플로우를 회귀 테스트로 고정.
- [ ] `TSK-FE-016` [P2] `types/user.ts`의 `User`(로그인 응답 매핑) 타입이 실제 API 응답과 불일치 (`TSK-FE-011` MyPage 연동에서 식별, 2026-08-27)
  - **배경**: `User` 인터페이스에 `pets: Pet[]`/`activePetId?: string` 필드가 있지만, 실제 로그인 응답(`UserLoginResponseDto.UserSummaryDto`)은 `id`/`email`/`nickname`/`avatarUrl`/`role`만 내려주며 `pets`/`activePetId`는 응답에 존재한 적이 없다(항상 빈 배열/undefined로만 취급되어 왔음, `PetPage`/`SearchPage`가 각자 `GET /pets`를 직접 호출하는 것도 이 때문으로 추정). `Pet`(`TSK-FE-010`) 때와 달리 이번에는 `authStore`/여러 페이지의 로그인 mock 데이터가 이 타입에 의존하고 있어 영향 범위가 넓아, 이번 작업 범위에서는 임의로 정리하지 않고 백로그로만 기록.
  - **DoD**: `User` 타입에서 실제로 내려오지 않는 `pets`/`activePetId`를 제거하거나, 실제로 필요하다면 `role`을 타입에 추가하고 `pets` 관련 필드는 별도 API 응답으로 명확히 분리. 변경 시 `authStore`, 관련 스펙(`PlaceDetailPage.spec.ts` 등)의 로그인 mock 데이터도 함께 정리.
- [x] `TSK-FE-017` [P1] 지도에 장소 마커 연동 (`TSK-FE-007` 0단계 이후 다음 단계, 2026-09-09 완료)
  - **배경**: 2026-08-27에 한 차례 기존 `GET /places/search` 재사용 마커 표시 + 마커 클릭 카드 + 상세 이동까지 구현했었으나(`4a4b00c`), 이후 요청에 따라 "SDK 로딩/지도 렌더링 자체 검증"에만 집중하도록 의도적으로 되돌렸다(`MapPage.vue`에서 장소 조회/마커 코드 제거). 2026-09-09에 `MapPage.vue`가 다시 장소 마커 연동을 포함한 상태로 확인되어(사전 확보 작업으로 추정), 이번 작업에서 요구사항 충족 여부를 재검증하고 완료로 기록.
  - **DoD**: 기존 `GET /places/search` 재사용(신규 API 없음)해 지도 중심 기준 장소 마커 표시, 마커 클릭 시 최소 정보 카드(장소명/카테고리/평점/리뷰수), 카드에서 `/place/:id` 이동. 뷰포트 기준 재조회(지도 이동/확대·축소 시 디바운스 재검색)는 이번 범위에서 명시적으로 제외하고 `TSK-FE-018`로 별도 기록(아래 참고).
- [ ] `TSK-FE-018` [P3] 지도 뷰포트 이동/확대축소 시 디바운스 재검색 (`TSK-FE-017`에서 범위 제외, 2026-09-09 신규)
  - **배경**: `TSK-FE-017` 요구사항에서 지도 드래그/이동 시 재검색은 명시적으로 범위 밖으로 지정되었다. 현재 `MapPage.vue`는 최초 마운트 시 1회만 `GET /places/search`를 호출한다.
  - **DoD**: 지도 `dragend`/`zoom_changed` 이벤트에 디바운스를 걸어 새 중심 좌표 기준으로 재검색, 최소 이동 거리 임계값 적용, 마커 클러스터링도 함께 검토(마커 수가 많아질 경우 대비).
- [ ] `TSK-BE-031` [P2] `Place` 엔티티의 실내/실외/목줄 자유구역 정책 필드가 API 응답에 노출되지 않음 (`TSK-FE-008` 반려동물 동반 수칙 보강에서 식별, 2026-08-27)
  - **배경**: `Place` 엔티티에는 `isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone` 컬럼이 이미 존재하지만, `PlaceSearchResponseDto`(검색/상세 공용 Projection)에는 `allowedSizes`/`maxWeightLimitKg`만 포함되어 있어 프런트엔드가 이 값을 받을 수 없다. 이번 작업(장소 상세 반려동물 동반 수칙 섹션)에서는 요구사항에 따라 실제 API가 제공하는 필드만 표시했고, 임의로 DTO를 확장하지 않았다.
  - **DoD**: 실내/실외/목줄 자유구역 정보를 반려동물 동반 수칙에 노출할 필요가 있는지 제품 우선순위를 확인한 뒤, 필요 시 `PlaceSearchResponseDto`에 해당 필드 추가(QueryDSL Projection 컬럼 추가) 및 프런트엔드 표시 UI 구현.
- [ ] `TSK-BE-032` [P2] 장소 평점(`rating`) 기본값 불일치 — 리뷰 0건인데 신규 생성 시 5.0, 리뷰 전부 삭제 시 0.0 (`TSK-BE-022` 보강 중 식별, 2026-11-19)
  - **배경**: `Place` 생성자는 `rating`이 `null`이면 `5.0`으로 채우지만(리뷰가 하나도 없는데 만점으로 표시됨), `ReviewService.updatePlaceRatingAndReviewCount()`는 남은 리뷰가 0건이면 `0.0`으로 재계산한다. 같은 "리뷰 0건" 상태가 생성 경로에 따라 5.0/0.0으로 달라지고, TourAPI로 적재되는 모든 신규 장소가 평점 5.0으로 노출된다(평점 기준 정렬/인기 장소에서 리뷰 없는 장소가 상위에 올 수 있음). `Sprint2FullFlowIntegrationTest`는 현재 동작(생성 5.0 → 삭제 후 0.0)을 그대로 고정해 두었다.
  - **DoD**: "리뷰 없음"의 표현을 하나로 통일(권장: `rating=null` 또는 0.0 + 프런트에서 "평점 없음" 표시)하고, 기존 데이터 마이그레이션 여부 결정, 대시보드 인기/추천 정렬 기준 영향 확인, 위 통합 테스트 기대값 갱신.
- [ ] `TSK-BE-033` [P1] 프로필 수정(`PUT /api/v1/users/me`) 시 닉네임 중복 검증 누락 (`TSK-BE-023` 에러 명세 작성 중 식별, 2026-11-19)
  - **배경**: 회원가입은 `existsByNickname`으로 중복을 막지만(`DUPLICATE_NICKNAME` 409), `UserProfileService.updateProfile()`은 중복 확인 없이 닉네임을 변경한다. DB에도 `nickname` UNIQUE 제약이 없다. 아이디 찾기(`POST /api/v1/auth/find-id`)는 닉네임으로 `findFirstByNicknameAndStatus`를 조회하므로, 중복 닉네임이 생기면 **다른 회원의 (마스킹된) 이메일이 반환될 수 있다**.
  - **DoD**: 프로필 수정 시 본인 제외 닉네임 중복 검사 + 409 응답(`@ApiErrorCodes`에 `DUPLICATE_NICKNAME` 추가), 기존 중복 데이터 점검 후 `nickname` UNIQUE 제약 추가(Flyway) 검토, 테스트 추가.
- [ ] `TSK-BE-034` [P2] 회원가입과 비밀번호 변경의 비밀번호 규칙 불일치 (`TSK-BE-023` 에러 명세 작성 중 식별, 2026-11-19)
  - **배경**: `ChangePasswordRequestDto`는 `@Pattern`(영문+숫자+특수문자, 8~100자)을 강제하지만 `UserRegisterRequestDto`는 `@Size(8~100)`만 검사한다. 프런트엔드 회원가입 화면은 패턴을 검사하므로 정상 사용 시에는 드러나지 않지만, API를 직접 호출하면 약한 비밀번호로 가입할 수 있다.
  - **DoD**: 회원가입 DTO에 동일한 `@Pattern` 적용(또는 공통 커스텀 어노테이션으로 통합), 테스트 추가.
- [ ] `TSK-BE-035` [P3] 비즈니스 예외가 `ErrorCode`/`BusinessException`을 사용하지 않음 (Refactoring Sprint "Exception 개선" 항목의 구체화, 2026-11-19)
  - **배경**: `ErrorCode` enum과 `BusinessException`이 정의되어 있지만 실제 예외 클래스(`PlaceNotFoundException` 등)는 모두 `RuntimeException`을 직접 상속하고, 메시지가 서비스 코드 곳곳에 하드코딩되어 있다(예: "장소를 찾을 수 없습니다." vs "장소 정보를 찾을 수 없습니다."). `GlobalExceptionHandler`도 예외 타입마다 처리기를 하나씩 두고 있다. `TSK-BE-023`에서는 Swagger 에러 명세의 기준으로 `ErrorCode`를 사용했으므로, 런타임 메시지와 문서 예시 사이에 문구 차이가 일부 있다.
  - **DoD**: 예외 클래스가 `BusinessException(ErrorCode)`를 상속하도록 통일, `GlobalExceptionHandler`를 `BusinessException` 단일 처리기로 단순화, 필요 시 응답 본문에 에러 코드 필드 추가(프런트엔드 영향 검토).
- [x] `TSK-DATA-002` [P0] 한국관광공사 TourAPI(반려동물 동반여행 서비스) 클라이언트 실제 검증 및 스키마 확정 (2026-09-09 완료, 아래 "완료 기록" 참조)
  - **범위**: `infrastructure/publicdata/` 기존 클라이언트(`TourApiClient`/DTO/Mapper/Service/Batch)에 대한 실제 HTTP 호출 기반 검증, `baseUrl`/오퍼레이션 경로 오류 수정, 게이트웨이 오류 응답 처리 보강, 오프라인 테스트 추가. **DB 대량 적재/시드/마이그레이션은 이번 범위에서 명시적으로 제외**(→ TSK-DATA-003 이후로 이관).
  - **핵심 발견**: `application.yml` 기본 `baseUrl`이 `B551011/KorPetTourService`(오퍼레이션 무관하게 항상 `NO_OPENAPI_SERVICE_ERROR`/reasonCode 12 = 존재하지 않는 경로)로 잘못되어 있었음 → `B551011/KorPetTourService2`로 수정. 로컬 프로필 시크릿은 확인된 유효 경로(`areaBasedList2` 등)에 대해서도 `SERVICE_KEY_IS_NOT_REGISTERED_ERROR`(reasonCode 30)를 반환해, 실제 반려동물 필드 payload는 이번 작업에서 확보하지 못함(아래 백로그 `TSK-DATA-004` 참조). 기존 `TourApiClient`가 존재하지 않는 오퍼레이션(`/detailPetTour1`)을 호출하고 있던 버그도 함께 발견/수정.
- [x] `TSK-DATA-004` [P0] TourAPI 로컬 프로필 서비스키 활용신청 상태 확인 (2026-09-09 완료, 아래 "완료 기록" 참조)
  - **결론**: 활용신청 **승인완료**로 확인됨(data.go.kr 마이페이지 캡처로도 처리상태 "승인", 활용기간 2026-09-09~2028-09-09 재확인). `TSK-DATA-002` 시점에는 `SERVICE_KEY_IS_NOT_REGISTERED_ERROR`(reasonCode 30)가 발생했으나, 이후 두 차례 재확인(`areaBasedList2`, numOfRows=1 단건 호출) 모두 `resultCode 0000`으로 정상 응답(실제 장소 데이터 반환)했다. 원인은 활용신청 승인 처리 지연이었던 것으로 확정 — 코드/설정 문제가 아니었음. 실제 성공 응답 원문은 `backend/src/test/resources/tourapi/success-area-based-list2-real-2026-09-09.json`에 fixture로 저장(serviceKey 미포함). `TSK-DATA-005`(반려동물 필드 실제 검증) 착수 가능.
- [x] `TSK-DATA-005` [P2] TourAPI 반려동물 전용 필드 실제 스키마 검증 (2026-09-14 완료, 아래 "완료 기록" 참조)
  - **결론**: `areaBasedList2`/`detailIntro2`/`detailInfo2`/`detailCommon2` 4개 오퍼레이션을 실제 승인 키로 호출한 결과, 기존 코드의 추측 필드(`acmpyTypeCd`/`claAcmpyInfo`/`relaAcmpyInfo`/`relaPosbleFclty`/`relaRqustFclty`/`etcAcmpyInfo`/`acmpyPsblCpam`)는 **전부 미존재로 확인됨**(contentTypeId=38/쇼핑, 32/숙박 두 종류 교차 검증). 체중 제한/크기/목줄/이동장/실내외/오프리쉬 필드도 전부 미존재. 반려동물 관련 필드는 `detailIntro2`의 `chkpetshopping`(contentTypeId=38 한정, 관찰된 샘플은 빈 값) 하나만 확인됨 — Place 필드 자동 매핑에 쓸 만한 정보는 사실상 없음. 상세는 아래 완료 기록 참조.

### 2026-09-09 완료 기록: TourAPI(반려동물 동반여행 서비스) 클라이언트 실제 검증 (`TSK-DATA-002`)
- **실제 호출 결과 요약**: 로컬 프로필 시크릿(gitignored)으로 data.go.kr에 직접 HTTP 호출해 다음을 확인했다.
  - `B551011/KorPetTourService`(기존 `application.yml` 기본값, "2" 없음), `KorPetTourService1`, `PetTourService1/2`, `KorService1`(레거시), `detailPetTour1`/`detailPetTour2`/`areaBasedSyncList2` 오퍼레이션: 모두 `NO_OPENAPI_SERVICE_ERROR`(reasonCode 12) — 존재하지 않는 경로로 확인.
  - `B551011/KorPetTourService2` 하위 `areaBasedList2`/`locationBasedList2`/`detailCommon2`/`detailIntro2`/`detailImage2`/`searchKeyword2`: 전부 `SERVICE_KEY_IS_NOT_REGISTERED_ERROR`(reasonCode 30)로 응답 — 경로 자체는 유효하나(레거시/오오퍼레이션 오류와 다른 오류코드), 현재 로컬 키가 미승인/승인대기 상태로 추정(`TSK-DATA-004`로 기록). 동일 키로 무관한 `KorService2` 상품 호출 시에도 동일 오류가 발생해 인코딩 문제가 아님을 확인(Encoding/Decoding 키 양쪽 테스트).
  - 결과적으로 실제 아이템 payload(성공 응답)는 확보하지 못했다 — **반려동물 전용 8개 필드는 "여전히 확인 불가"(키 미승인)** 로 정직하게 기록하며, 이전에 "확인 필요"였던 상태에서 진전은 "표준 봉투 스키마 확정" 및 "게이트웨이 오류 스키마(별도 `OpenAPI_ServiceResponse` 루트) 실체 확인"까지다.
- **클라이언트 수정 내역**: `application.yml` 기본 `baseUrl`을 `KorPetTourService2`로 수정. `TourApiClient`의 하드코딩된 잘못된 오퍼레이션(`/detailPetTour1`, 실존하지 않음)을 확인된 유효 오퍼레이션(`/areaBasedList2`)으로 교체. data.go.kr이 정상 스키마(`response.header/body`)와 전혀 다른 루트 키(`OpenAPI_ServiceResponse.cmmMsgHeader`)의 게이트웨이 오류를 반환할 수 있음을 실제로 확인하고, 이를 파싱해 실제 오류코드/메시지를 노출하는 `TourApiErrorEnvelopeDto` 및 예외 처리 경로 추가(기존에는 이 경우 "Header Null"이라는 의미 없는 메시지만 던졌음). `TourApiPlaceItemDto`의 mapx/mapy를 `Double` 직접 매핑에서 안전 파싱 `String`(빈 문자열/숫자 아닌 값 대응, 페이지 전체 역직렬화 실패 방지)으로 변경.
- **테스트**: 신규 오프라인 테스트 10개 추가(DTO 역직렬화 4, 매퍼 좌표/체중 예외 케이스 3, 클라이언트 게이트웨이 오류 3) + 기존 10개 전부 유지. 실제 API 호출 통합 테스트(`TourApiClientRealApiIntegrationTest`)는 `-DrunTourApiIntegrationTest=true` 없이는 항상 스킵되도록 게이팅. 전체 백엔드 `./gradlew test`: **175개 중 174 통과, 1 스킵(의도된 게이팅), 실패/에러 0건**.
- **범위 외 확인**: DB 대량 적재/시드/마이그레이션은 수행하지 않았음(`TSK-DATA-003`+ 로 유지). `Place` 엔티티 필드 변경도 하지 않았음(현재 매핑 가능한 필드만으로 충분, 반려동물 전용 원본 코드를 저장할 컬럼이 필요한지는 `TSK-DATA-005` 해결 이후 재검토).

### 2026-09-09 완료 기록: TourAPI 로컬 프로필 서비스키 활용신청 상태 확인 (`TSK-DATA-004`)
- **점검 범위**: `Place` 엔티티/DB migration/대량 데이터 적재/프런트엔드/반려동물 필드 스키마 확정은 이번 작업 범위에서 명시적으로 제외. 서비스키 활용신청 상태와 API 키 전달 설정만 확인.
- **키 주입 방식 확인**: `TourApiClient`가 `@Value("${public-data.tour-api.service-key:test-service-key}")`로 주입받는 구조를 재확인. `application.yml`에는 플레이스홀더(`test-service-key`)만 있고, 실제 값은 `application-local.yml`(gitignored, Spring `local` 프로필에서만 로드)에만 존재 — 코드/문서 어디에도 실제 키 값 미기록.
- **API 호출 결과**: `GET /B551011/KorPetTourService2/areaBasedList2` (`numOfRows=1&pageNo=1&MobileOS=ETC&MobileApp=PetSpot&arrange=A&_type=json`) 총 2회(1차 최종 확인, 2차 재확인) 호출 → 매번 **HTTP 200, resultCode 0000, 실제 장소 데이터(부산 소재 장소 1건, contentid=2930927) 정상 반환**. `TSK-DATA-002`에서 발생했던 `SERVICE_KEY_IS_NOT_REGISTERED_ERROR`(reasonCode 30)는 더 이상 재현되지 않음.
- **활용신청 상태(포털 확인)**: data.go.kr 마이페이지 캡처로 "한국관광공사_반려동물_동반여행_서비스" 처리상태 = **승인**, 심의여부 = 자동승인, 활용기간 2026-09-09~2028-09-09 확인됨.
- **확인된 원인**: 서비스키 자체나 URL 인코딩/전달 방식의 문제가 아니라, `TSK-DATA-002` 수행 시점에 활용신청이 아직 **승인 처리 중**이었던 것으로 확정. 현재는 승인완료 상태로 정상 동작.
- **실제 응답 필드(이번에 확인된 것만)**: `contentid`, `contenttypeid`, `title`, `addr1`, `addr2`(빈 문자열), `tel`(빈 문자열), `mapx`, `mapy`, `firstimage`, `firstimage2`, `areacode`(빈 문자열), `sigungucode`(빈 문자열), `cat1`/`cat2`/`cat3`(빈 문자열), `modifiedtime`, `createdtime`, `cpyrhtDivCd`, `mlevel`, `zipcode`, `lDongRegnCd`, `lDongSignguCd`, `lclsSystm1`/`2`/`3`. **반려동물 전용 필드(`acmpyTypeCd` 등)는 이 응답에 전혀 존재하지 않음** — 다른 오퍼레이션(상세 등)에서 제공되는지는 미확인이며 `TSK-DATA-005`에서 별도 확인 필요(이번 작업에서 반려동물 필드 매핑은 확정하지 않음).
- **mapx/mapy 참고 확인**(범위 외, 부수적으로 관찰): `mapx="129.056805686045"`(경도), `mapy="35.1579550616706"`(위도)로 부산 지역 좌표와 일치 — `TSK-DATA-002`의 mapx=longitude/mapy=latitude 가정과 일치하는 정황.
- **fixture 저장**: 실제 성공 응답 원문을 `backend/src/test/resources/tourapi/success-area-based-list2-real-2026-09-09.json`에 저장(serviceKey 미포함). 기존 `success-area-based-list2-sample.json`(TSK-DATA-002 당시 가정치 샘플)은 그대로 유지 — 실물 데이터로 교체하는 것은 TSK-DATA-005 범위.
- **TSK-DATA-005 진행 가능 여부**: **가능** — 키는 정상 작동하므로 다음 단계에서 반려동물 전용 필드가 포함되는 오퍼레이션을 찾아 실제 스키마를 검증할 수 있음. 단, 이번 작업에서는 반려동물 필드 검증을 수행하지 않았으므로 `TSK-DATA-005`는 별도로 진행 필요.
- **테스트**: 코드 변경 없음(설정/구조 확인, fixture 추가, 총 2회 실제 API 호출만 수행) — 기존 백엔드 테스트 스위트에 영향 없음.

### 2026-11-19 완료 기록: Sprint 2 마감 — 통합 테스트 보강(`TSK-BE-022`) 및 Swagger 문서 정리(`TSK-BE-023`)
- **`TSK-BE-022` 통합 테스트 보강** (`Sprint2FullFlowIntegrationTest`)
  - 기존 테스트는 리뷰 응답의 평점만 확인하고 장소(Place) 평점 갱신은 검증하지 않았다. 또한 신규 장소의 평점 기본값이 5.0이라 5점 리뷰로는 갱신 여부를 구분할 수 없었다.
  - 리뷰를 3점으로 작성 → `GET /api/v1/places/{id}`로 `rating=3.0, reviewCount=1` 확인 → 리뷰를 4점으로 수정(`PUT /api/v1/reviews/{id}`, 신규 단계) → `rating=4.0` 확인 → 리뷰 삭제 후 `rating=0.0, reviewCount=0` 확인 단계를 추가했다. 마지막 값(0.0)은 현재 동작을 고정한 것으로, 기본값 불일치는 `TSK-BE-032`로 기록.
- **`TSK-BE-023` Swagger(OpenAPI) 문서 정리**
  - **보안 스킴 버그 수정**: 문서 전역 보안 스킴 이름은 `BearerAuth`, 컨트롤러 API 대부분은 `bearerAuth`(소문자)를 참조하고 있어 Swagger UI에서 Authorize를 눌러도 대부분의 API에 토큰이 전송되지 않았다. `SwaggerConfig.SECURITY_SCHEME_NAME` 상수로 통일하고, 전역 보안을 제거해 공개 API(회원가입/로그인/장소 조회 등)에는 자물쇠가 표시되지 않게 했다.
  - **에러 응답 자동 명세**: `@ApiErrorCodes` 어노테이션(엔드포인트별 비즈니스 에러 선언)과 `ApiErrorResponseCustomizer`(공통 에러 자동 부착)를 추가. 공통 규칙: 입력이 있는 API는 400, 인증 API는 401, 비밀번호 강제 변경 인터셉터 대상 API는 403(`AUTH_004`), 전체 API는 500. 같은 상태 코드의 에러는 하나의 응답으로 묶고 에러 코드별 예시(`{"success":false,"data":null,"error":"...","timestamp":"..."}`)를 표시. 공통 `ErrorResponse` 스키마 등록. `ErrorCode`에 `PASSWORD_CHANGE_REQUIRED`(AUTH_004), `DUPLICATE_NICKNAME`(USER_002) 추가. 인터셉터 제외 경로는 `WebMvcConfig` 상수로 추출해 문서와 실제 설정이 같은 값을 사용.
  - **201 응답 반영**: 실제로 201을 반환하는 생성 API 4개(회원가입/반려동물 등록/즐겨찾기 추가/리뷰 작성)가 문서에는 200으로 표시되던 문제를 `@ResponseStatus(HttpStatus.CREATED)`로 수정.
  - **요청/응답 예시**: 요청·응답 DTO 22개(중첩 DTO 포함)의 139개 필드에 `@Schema(description, example)` 추가. 정책 필드(`allowedSizes`, `maxWeightLimitKg`)에는 ADR-007의 null 의미(미검증)를 명시.
  - **입력 형식 오류 400 처리 (버그 수정)**: 잘못된 UUID 경로 변수(`MethodArgumentTypeMismatchException`)와 깨진 JSON 본문(`HttpMessageNotReadableException`)이 처리기 부재로 500을 반환하던 문제를 `GlobalExceptionHandler`에서 400으로 처리하도록 수정(내부 파서 메시지는 노출하지 않음).
  - **문서-실제 일치 검증 테스트**: `OpenApiDocumentationIntegrationTest`(7건) — 생성된 `/v3/api-docs`를 읽어 보안 스킴 참조, 문서의 인증 표시와 실제 401 동작 일치(전 API를 토큰 없이 실제 호출), 공통/비즈니스 에러 예시, 403 적용 범위, 201 표시, DTO 예시 존재를 검증. `InvalidRequestFormatIntegrationTest`(2건) — 잘못된 UUID/JSON의 400 응답 검증.
- **검증**: 전체 `./gradlew test`: 198 tests, 실패/에러 0건, 2 skip(의도된 opt-in 실 API 테스트).
- **식별된 후속 과제**: `TSK-BE-032`(평점 기본값 불일치), `TSK-BE-033`(프로필 수정 닉네임 중복 미검증, P1), `TSK-BE-034`(회원가입 비밀번호 규칙 불일치), `TSK-BE-035`(ErrorCode 기반 예외 구조 통일).

### 2026-09-14 완료 기록: TourAPI 반려동물 필드 실제 스키마 검증 (`TSK-DATA-005`)
- **범위**: 실제 API 오퍼레이션 호출을 통한 반려동물 전용 필드 스키마 검증만 수행. DB 대량 적재/Place 엔티티 변경/DB migration/프런트엔드/검색 API/지도 기능/Redis/Elasticsearch/AI 추천/전체 데이터 수집/배치 스케줄러/임의 반려동물 정책 데이터 생성은 전부 범위 밖으로 명시적으로 제외.
- **사용한 실제 데이터**: `TSK-DATA-004` fixture(`success-area-based-list2-real-2026-09-09.json`)의 `contentid=2930927`/`contenttypeid=38`(쇼핑, "가까운약국"). 추가로 `contenttypeid`별 필드명 차이를 교차 검증하기 위해 `areaBasedList2`(contentTypeId=32/숙박)에서 단건("가영당", `contentid=2707444`)을 추가로 조회해 총 2개 contentId로 확인. 대량 호출 없이 각 오퍼레이션당 단건씩만 호출.
- **호출 결과(전부 HTTP 200 / resultCode 0000 / resultMsg OK)**:
  1. `detailIntro2` (contentId=2930927, contentTypeId=38/쇼핑): 성공. 필드는 `saleitem`/`fairday`/`restroom`/`infocentershopping`/`parkingshopping`/`chkbabycarriageshopping`/`chkpetshopping`/`chkcreditcardshopping`/`opentime` 등. **반려동물 관련 필드는 `chkpetshopping` 하나뿐**(이 항목의 실제 값은 빈 문자열 — 미기재).
  2. `detailIntro2` (contentId=2707444, contentTypeId=32/숙박): 성공. 필드는 `roomcount`/`roomtype`/`checkintime`/`checkouttime`/`parkinglodging`/`pickup`/`barbecue` 등 숙박 전용 필드. **`chkpet`류 필드 자체가 없음** — 쇼핑 타입과 반려동물 필드 유무 자체가 다름(오퍼레이션 필드셋이 `contentTypeId`에 종속됨을 확인).
  3. `detailInfo2` (contentId=2930927, contentTypeId=38): 성공이지만 `items`가 빈 문자열("")이고 `totalCount=0` — 이 contentId에는 반복정보 자체가 없음(데이터 없음이지 반려동물 정보 부재 확정은 아님).
  4. `detailInfo2` (contentId=2707444, contentTypeId=32): 성공. 반복정보는 "객실별 상세"(`roomtitle`/`roomsize1`/`roomcount`/`roombasecount`/`roommaxcount`/요금/옵션 등) — 반려동물 관련 필드 없음.
  5. `detailCommon2` (contentId=2930927): `defaultYN`/`firstImageYN`/`areacodeYN`/`catcodeYN`/`addrinfoYN`/`mapinfoYN`/`overviewYN` 파라미터를 붙이면 `resultCode=10 INVALID_REQUEST_PARAMETER_ERROR(defaultYN)` — 이 서비스의 `detailCommon2`는 해당 옵션 파라미터를 지원하지 않음(공식 TourAPI4.0 공통 문서와 다른 부분). 파라미터 없이 재호출하여 성공. 필드는 `contentid`/`contenttypeid`/`title`/`addr1`/`addr2`/`tel`/`homepage`/`firstimage`/`firstimage2`/`areacode`/`sigungucode`/`cat1~3`/`mapx`/`mapy`/`overview`/`modifiedtime`/`createdtime` 등 — `areaBasedList2`와 겹치는 필드 다수(특히 `mapx`/`mapy`/`addr1`/`title`은 완전히 동일한 값으로 중복). 반려동물 필드 없음. `overview`(자유 텍스트)에도 반려동물 관련 언급 없음(2건 모두 확인).
- **좌표 검증**: `detailCommon2`의 `mapx="129.056805686045"`, `mapy="35.1579550616706"`는 `Double.parseDouble`로 정상 변환되며, `areaBasedList2` fixture의 동일 contentId 값과 완전히 일치 — `mapx=경도/mapy=위도` 가정이 실제로 확인됨. 좌표가 빈 문자열인 항목(기존 `success-area-based-list2-sample.json`의 가정 케이스)에 대한 안전 파싱 필요성도 여전히 유효 — 현재 `TourApiPlaceItemDto`의 String 기반 안전 파싱 구조는 **유지**.
- **Place 필드 매핑 가능성 판정**:
  - `maxWeightLimitKg`: **D (판단 불가/사실상 C)** — 확인된 4개 오퍼레이션 어디에도 체중 제한 관련 필드 없음.
  - `allowedSizes`: **D** — 크기 관련 필드 없음.
  - `isIndoorAllowed`/`isOutdoorAllowed`: **D** — 실내/실외 관련 필드 없음.
  - `hasOffLeashZone`: **D** — 오프리쉬 관련 필드 없음.
  - 참고로 `chkpetshopping`(쇼핑 타입 한정, "반려동물 동반 가능 여부"로 추정되나 Y/N 코드 정의가 공식 문서로 확인되지 않았고 관찰된 값도 빈 문자열이라 **B로도 판정하지 않음** — 이 필드 하나만으로는 어떤 Place 필드도 채울 수 없음).
- **기존 추측 필드 처리**: 삭제하지 않고 `TourApiPlaceItemDto` 클래스 상단 Javadoc에 검증 결과를 기록만 함(각 필드 "실제 응답 미존재"로 명시). 코드/Entity 변경은 이번 작업 범위 밖이라 보류 — 제거는 별도 후속 작업(`TSK-DATA-007`, 아래 백로그) 필요.
- **fixture 저장**(전부 serviceKey 미포함, `backend/src/test/resources/tourapi/`):
  - `success-detail-intro2-real-2026-09-14.json` (쇼핑)
  - `success-detail-intro2-lodging-real-2026-09-14.json` (숙박, 교차 검증용)
  - `success-detail-info2-real-2026-09-14.json` (쇼핑, 반복정보 없음 케이스)
  - `success-detail-info2-lodging-real-2026-09-14.json` (숙박, 객실 반복정보)
  - `success-detail-common2-real-2026-09-14.json` (쇼핑)
- **테스트**: 신규 오프라인 테스트 클래스 `TourApiDetailOperationsRealSchemaTest`(5개, JsonNode 기반으로 실제 필드 존재/부재 및 mapx/mapy 파싱 검증) 추가. 실제 API 호출 통합 테스트는 기존 `TourApiClientRealApiIntegrationTest` 패턴(`-DrunTourApiIntegrationTest=true` 없이는 항상 스킵)을 그대로 유지, 신규 통합 테스트는 추가하지 않음(이번 검증은 1회성 수동 호출로 충분). 전체 `./gradlew test` 통과(실패/에러 0건).
- **완료 조건 질문 답변**:
  1. 반려동물 동반 여부 제공 오퍼레이션: `detailIntro2` (단, `contentTypeId=38`/쇼핑에서만 확인됨, 32/숙박에는 없음 — 전체 `contentTypeId`에 대한 일반화는 **확인 불가**).
  2. 실제 필드명: `chkpetshopping` (쇼핑 타입 한정).
  3. 값의 형태: 문자열, 관찰된 샘플은 빈 문자열(미기재) — Y/N 등 구체적 코드 정의는 **확인 불가**(공식 문서 미대조, 값 있는 샘플 미확보).
  4. 반려동물 크기 정보: **없음**(확인된 4개 오퍼레이션 기준).
  5. 체중 제한 정보: **없음**.
  6. 목줄/이동장 정보: **없음**.
  7. 실내/실외 정보: **없음**.
  8. 오프리쉬 정보: **없음**.
  9. `acmpyTypeCd` 등 추측 필드 실제 존재 여부: **미존재로 확인됨**(7개 필드 전부).
  10. `mapx`/`mapy`가 실제로 longitude/latitude인가: **예, 확인됨**(`detailCommon2` 실제 값이 `areaBasedList2`와 일치하고 부산 실좌표와 부합).
  11. 현재 Place의 각 반려동물 필드를 TourAPI만으로 채울 수 있는가: **아니오** — 확인된 오퍼레이션 범위 내에서는 어떤 필드도 신뢰성 있게 채울 수 없음(별도 데이터 소스 필요, 판정 C).
- **후속 백로그**: `TSK-DATA-007`(아래, 이번 정리로 번호 조정 — `TSK-DATA-006`은 "Source of Truth/보강 전략 설계" 작업이 별도로 배정되어 필드 정리 작업을 `007`로 이관) — 미존재 확인된 추측 필드 제거 및 `TourApiPlaceMapper`의 관련 로직(정규식 추출 등) 재검토.

- [x] `TSK-DATA-006` [P1] PetSpot 반려동물 정책 데이터의 Source of Truth 및 보강 전략 설계 (2026-09-14 완료, 아래 "완료 기록" 참조)
  - **결론**: TourAPI = 장소 기본정보 Source of Truth, PetSpot(운영자 검증/관리자 입력) = 반려동물 정책 데이터 Source of Truth로 분리. `null`(미검증)/`false`(근거 있는 불가)/`true`(근거 있는 가능) 3단계 원칙을 채택하되, 현재 `TourApiPlaceMapper`와 `Place` 생성자가 이 원칙을 위반(정책 필드에 근거 없는 기본값을 채움)하고 있음을 발견 — 수정은 `TSK-DATA-008`로 이관. `PASS`/`WARN`/`DENY` 판정에서 정책 필드가 하나라도 `null`이면 보수적으로 `WARN` 처리하는 원칙 수립. `TSK-DATA-003`(초기 적재) 전략은 "TourAPI 기본정보 우선 적재 + 정책은 `null`로 유지 + `WARN` 노출"을 권장(단 `TSK-DATA-008` 선행 필요). 상세는 `docs/DECISIONS.md`의 **ADR-007** 참조.
  - **범위**: 설계/문서화만 수행. Entity/migration/대량 적재/배치/Frontend/PASS-WARN-DENY 실제 구현/AI/Redis/Elasticsearch는 전부 범위 밖으로 제외.

- [ ] `TSK-DATA-007` [P2] TourAPI 미존재 확인된 반려동물 추측 필드 정리 (`TSK-DATA-005`에서 식별, 2026-09-14)
  - **배경**: `TSK-DATA-005`에서 `TourApiPlaceItemDto`의 `acmpyTypeCd`/`claAcmpyInfo`/`relaAcmpyInfo`/`relaPosbleFclty`/`relaRqustFclty`/`etcAcmpyInfo`/`acmpyPsblCpam` 7개 필드가 `areaBasedList2`/`detailIntro2`/`detailInfo2`/`detailCommon2` 어디에도 존재하지 않음이 실제 응답으로 확인되었다. 범위 제한(Entity/매핑 로직 변경 금지)으로 이번 작업에서는 문서화만 하고 코드는 그대로 두었다.
  - **DoD**: 위 7개 필드 및 이를 참조하는 `TourApiPlaceMapper`의 정규식 추출 로직(체중 등)을 제거하거나, PetSpot 자체 정책 데이터로 대체할 별도 소스가 결정되면 그에 맞게 교체. `success-area-based-list2-sample.json`(가정치 샘플, `acmpyTypeCd="PET002"` 등 포함)도 실물 스키마와 맞지 않으므로 함께 정리 검토.

- [x] `TSK-DATA-008` [P1] `TourApiPlaceMapper`/`Place` 생성자의 반려동물 정책 필드 근거 없는 기본값 채움 제거 (2026-09-14 완료, 아래 "완료 기록" 참조)
  - **결론**: `Place` 생성자의 null→기본값 자동 치환 로직(`allowedSizes`→전체 크기, `isIndoorAllowed`/`isOutdoorAllowed`→`true`, `hasOffLeashZone`→`false`)과 `TourApiPlaceMapper`의 하드코딩(`isIndoorAllowed(true)`/`isOutdoorAllowed(true)`/`hasOffLeashZone(false)`)을 모두 제거해 `maxWeightLimitKg`와 동일하게 null을 그대로 유지하도록 수정. `PlaceSearchSizeAndDistanceIntegrationTest`의 암묵적 기본값 의존 테스트 케이스를 명시적 값 지정으로 수정하고, null 정책 필드가 저장/조회/크기필터 검색에서 예외 없이 동작함(단, 근거 없는 장소는 크기 필터에서 자동 제외됨)을 검증하는 테스트를 추가. 전체 `./gradlew test`: **183개 중 182 통과, 1 스킵(의도된 게이팅), 실패/에러 0건**. 상세는 아래 완료 기록 참조.

- [x] `TSK-DATA-003-A` [P0] TourAPI 초기 데이터 적재 최소 파이프라인 구축(최대 5건) (2026-09-14 완료, 아래 "완료 기록" 참조)
  - **결론**: 기존 `TourDataCollectorService.collectPage()`/`saveCollectedPlaces()` + `TourApiPlaceMapper`/`PlaceRepository.findByPublicDataId()` 기반 중복 방지 파이프라인을 신규 코드 추가 없이 그대로 재사용해, `areaBasedList2`(numOfRows=5) 실제 데이터가 로컬 PostgreSQL/PostGIS `places` 테이블에 정상 저장됨을 실제 DB 기반 테스트로 검증했다. 장소 기본정보는 정상 저장되고, 반려동물 정책 5개 필드는 전부 `null`로 저장됨(TSK-DATA-008 원칙 실제 적용 확인)을 확인했다. 동일 배치 재실행 시 `publicDataId` 기준으로 중복 생성 없이 업데이트 처리됨을 확인했다. **구조적 제약 발견**: `Place.latitude`/`longitude`/`location`은 DB `NOT NULL` 컬럼이라 좌표를 문자 그대로 `null`로 저장할 수 없어, 좌표 누락/파싱 실패 시 기존 TSK-DATA-002 정책대로 `0.0`/`0.0`(Null Island)로 대체 저장된다 — migration 없이는 해소 불가하므로 이번 범위에서는 그대로 두고 백로그로 기록. 상세는 아래 완료 기록 참조.
  - **범위**: 전국/대량 적재, 스케줄러 자동 실행, Redis/Elasticsearch/Docker/AWS/Frontend 변경 없음. 최대 5건만 대상.

- [x] `TSK-DATA-003-B` [P0] 좌표 결측 장소의 초기 적재 정책 결정 및 최소 구현 (2026-09-14 완료, 아래 "완료 기록" 참조)
  - **결론**: `TSK-DATA-003-A`에서 발견된 "좌표 결측 시 0.0/0.0(Null Island) 대체 저장" 구조적 문제에 대해, **전략 B(좌표 없는 장소는 Place 적재 자체에서 제외)** 를 채택했다. `Place.latitude`/`longitude`/`location`은 DB `NOT NULL`을 그대로 유지(migration 없음), `TourApiPlaceMapper.toEntity()`는 좌표를 파싱할 수 없으면 `null`을 반환하도록 수정, `TourDataCollectorService.saveCollectedPlaces()`는 `contentId` 결측과 동일하게 이 경우를 스킵하도록 수정. 기존 검색/지도 API는 전혀 변경하지 않았다(항상 좌표가 유효하다는 기존 불변식 유지). 상세 근거는 `docs/DECISIONS.md`의 **ADR-008** 참조.
  - **범위**: 전국 대량 적재 없음, migration 없음, Frontend 변경 없음, 불필요한 Entity/DB 구조 변경 없음.

### 2026-09-14 완료 기록: TourAPI Place Mapper 반려동물 정책 근거 없는 기본값 제거 (`TSK-DATA-008`)
- **범위**: `TSK-DATA-006`(ADR-007)에서 결정한 "정책 정보가 없으면 null로 유지한다" 원칙을 코드에 반영. DB migration/Place Entity 구조 변경/PetPolicy Entity 신설/Frontend 변경/PASS-WARN-DENY 엔진 구현/관리자 화면/사용자 제보/AI/TourAPI 대량 호출/초기 데이터 적재/배치 스케줄러/Redis/Elasticsearch는 전부 범위 밖으로 제외.

- **1) 발견된 버그(수정 전 실제 코드 확인 결과)**:
  - `TourApiPlaceMapper.toEntity()`(수정 전): `.isIndoorAllowed(true)`, `.isOutdoorAllowed(true)`, `.hasOffLeashZone(false)`를 TourAPI 응답 근거 없이 상수로 하드코딩하고 있었다. `allowedSizes`는 아예 설정하지 않아 `Place` 생성자의 기본값 치환에 의존하고 있었다.
  - `Place`의 `@Builder` 생성자(수정 전, `Place.java:126-129`): `allowedSizes`/`isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone`에 `null`이 들어오면 각각 `["SMALL","MEDIUM","LARGE"]`(전체 허용)/`true`/`true`/`false`로 **자동 치환**하고 있었다(`maxWeightLimitKg`만 유일하게 `null`을 그대로 유지). 이는 `Place.builder()`를 호출하는 **모든** 코드 경로(매퍼뿐 아니라 테스트의 수동 생성 등)에 공통 적용되는 구조적 문제였다.
  - 즉 근거는 `TourApiPlaceMapper`(호출 시점의 하드코딩)와 `Place` Entity 생성자(구조적 기본값 치환) 양쪽에 있었다 — 매퍼만 고쳐서는 `allowedSizes` 등이 여전히 생성자에서 전체 허용으로 치환되므로, Entity 생성자 수정이 불가피했다(사전에 최소 수정 필요성 확인 후 진행).

- **2) 수정 내용**:
  - `Place.java`: 생성자에서 `allowedSizes`/`isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone`의 null→기본값 치환 로직(및 이제 쓰이지 않는 `DEFAULT_ALLOWED_SIZES` 상수)을 제거하고, `maxWeightLimitKg`와 동일하게 입력값을 그대로 유지하도록 변경. 각 필드에 ADR-007 NULL 정책(`null`=미검증/`false`=근거 있는 불가/`true`=근거 있는 가능)을 명시한 Javadoc 추가. `rating`(기본 5.0)/`reviewCount`(기본 0) 등 정책과 무관한 필드의 기본값 로직은 그대로 유지(범위 밖).
  - `TourApiPlaceMapper.java`: `.isIndoorAllowed(true)`/`.isOutdoorAllowed(true)`/`.hasOffLeashZone(false)` 3줄을 제거(그대로 두면 `null`이 유지됨). `allowedSizes`는 원래도 설정하지 않았으므로 변경 없음(이제는 생성자가 이를 그대로 `null`로 유지).
  - `TourApiPlaceItemDto.java`: 변경 없음(범위 밖, `TSK-DATA-007`에서 처리 예정).

- **3) Entity가 null을 안전하게 지원하는지 확인 결과**:
  - DB 컬럼(`V1__init_schema.sql`): `allowed_sizes`/`is_indoor_allowed`/`is_outdoor_allowed`/`has_off_leash_zone`/`max_weight_limit_kg` 전부 `NOT NULL` 제약이 없어 DB 레벨에서는 원래도 null 저장이 가능했다(컬럼 `DEFAULT` 값은 INSERT 시 애플리케이션이 항상 값을 채워 보내므로 실질적으로 트리거되지 않음).
  - **실제 문제는 DB가 아니라 애플리케이션(Java) 계층**이었다 — `Place` 생성자가 DB에 도달하기도 전에 `null`을 기본값으로 치환하고 있었다. 이번 수정으로 이 계층의 치환 로직을 제거해, DB의 nullable 설정이 실제로 의미를 갖게 되었다.
  - `QueryDSL`/JPA(Hibernate `@JdbcTypeCode(SqlTypes.JSON)` 포함) 계층은 `null` 저장/조회에 별도 수정 없이 정상 동작함을 `PlaceSearchSizeAndDistanceIntegrationTest`의 신규 테스트(실제 PostgreSQL 대상)로 확인했다.

- **4) 기존 기능 영향 확인**:
  - Place search/detail: `PlaceSearchResponseDto`/`QPlaceSearchResponseDto`는 원래부터 `null` 값을 그대로 전달하는 단순 Projection이라 수정 불필요. `PlaceRepositoryImpl.sizeCategoryAllowed()`는 PostgreSQL `jsonb_exists(NULL, text)`가 `NULL`을 반환하는 특성상, `allowedSizes`가 `null`인 장소는 **크기 필터 검색에서 자동으로 제외**된다(예외 없이 안전하게 동작, 단 검색 결과에서 빠짐 — 신규 테스트로 실증). `maxWeightGte()`는 기존부터 `maxWeightLimitKg IS NULL OR maxWeightLimitKg >= 펫체중` 조건이라 `null`을 "제한 없음"으로 허용하는 기존 동작 그대로 유지(이번 작업이 변경한 범위 아님, 검색 API 로직 변경은 범위 밖).
  - Favorite/Review/My Page: 정책 필드를 참조하는 코드가 없음을 grep으로 확인(`getAllowedSizes`/`getIsIndoorAllowed`/`getIsOutdoorAllowed`/`getHasOffLeashZone` 등 프로덕션 코드 내 참조는 `Place`/`TourApiPlaceMapper`/`PlaceSearchResponseDto`/`PlaceRepositoryImpl` 외에는 없음) — 영향 없음.
  - `TourDataCollectorService`/`TourDataBatch`: 정책 필드를 직접 참조하지 않고 `TourApiPlaceMapper.toEntity()` 결과를 그대로 저장/갱신할 뿐이라 영향 없음(`TourDataCollectorServicePersistenceTest`는 매퍼를 Mock으로 대체하므로 이번 변경과 무관하게 통과).
  - `Boolean.TRUE`/`Boolean.FALSE`/`requireNonNullElse`/`getOrDefault`/전체 크기 허용 관련 패턴을 `domain/place`, `infrastructure/publicdata`, `application/place` 전역에 grep했으나 이번 버그와 관련된 추가 발견 없음(범위 외 리팩터링은 하지 않음).

- **5) 테스트**:
  - `TourApiPlaceMapperTest`에 2개 추가: ① TourAPI 기본 장소 데이터만 있는 DTO → `maxWeightLimitKg`/`allowedSizes`/`isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone` 전부 `null` 검증(기존 필드 매핑은 정상 유지되는지도 함께 검증). ② 정책 정보 없는 응답에서 `allowedSizes` 등에 임의 기본값이 자동 생성되지 않는지 별도 검증. 기존 3개 테스트(좌표/체중 관련)는 이번 변경과 무관해 그대로 통과.
  - `PlaceSearchSizeAndDistanceIntegrationTest`: 기존 "전체 견종 동반 가능 카페" 테스트 케이스가 `allowedSizes` 미지정 시의 암묵적 기본값(전체 허용)에 의존하고 있어, 이제 명시적으로 `List.of("SMALL","MEDIUM","LARGE")`를 지정하도록 수정(테스트 의도— "실제로 전체 크기가 확인된 장소"— 자체는 그대로 유지, 암묵적 의존만 제거). 신규 테스트 1개 추가: 정책 필드가 전부 `null`인 장소를 실제 PostgreSQL에 저장 → 저장/상세조회가 예외 없이 성공하고 `null`이 유지되는지, 크기 필터 검색에서는 근거 없음으로 제외되는지 검증.
  - `TourDataCollectorServicePersistenceTest`/`TourDataBatchTest`: 매퍼를 Mock으로 사용하거나 정책 필드를 다루지 않아 기존 테스트 그대로 통과(수정 불필요, 확인만 함).
  - 실제 API 키는 이번 작업에서 호출/로그/테스트/문서 어디에도 사용하지 않음(순수 코드 수정 + 기존 fixture 기반 오프라인 테스트만 사용).
  - 전체 `./gradlew test` 결과: **183 tests, 0 failures, 0 errors, 1 skipped**(의도된 게이팅 — `TourApiClientRealApiIntegrationTest`).

- **6) 대량 데이터 적재 전에 이 수정이 필요했던 이유**: `TSK-DATA-003`(초기 대량 적재)을 이 수정 없이 진행했다면, TourAPI에서 실제로 확인된 적 없는 "실내 가능/실외 가능/오프리쉬 불가/전체 크기 허용"이 모든 신규 장소에 사실처럼 저장되어, 반려동물 동반 불가 장소에서도 `PASS`로 오판되어 방문 거절 사고로 이어질 수 있었다(`docs/PRODUCT.md`의 핵심 제품 가치와 정면 상충). 이번 수정으로 정책 데이터가 없는 장소는 이제 정직하게 `null`(미검증)로 저장되며, `TSK-DATA-003` 착수의 선행 조건이 충족되었다.

### 2026-09-14 완료 기록: TourAPI 초기 데이터 적재 최소 파이프라인 구축 (`TSK-DATA-003-A`)
- **범위**: 전국 대량 적재/수천만 건 적재/전체 지역 반복 호출/스케줄러 자동 실행/매일 실행/Redis/Elasticsearch/Docker/AWS/Frontend는 전부 범위 밖으로 제외. 최대 5건만 대상으로 하는 최소 파이프라인 검증.

- **1) API 호출 방식**: `KorPetTourService2/areaBasedList2`, `numOfRows=5`, `pageNo=1`, `MobileOS=ETC`, `MobileApp=PetSpot`, `arrange=A`, `_type=json` — 기존 `TourApiClient.fetchPetPlaces(numOfRows, pageNo)`/`TourDataCollectorService.collectPage(5, 1)`를 신규 코드 없이 그대로 재사용(`collectAllPlaces()`는 `totalCount` 전체를 순회하므로 사용하지 않음 — `areaBasedList2`의 실제 `totalCount`는 9,683건이라 잘못 사용하면 대량 호출로 이어질 위험이 있어 의도적으로 배제).

- **2) DB 저장 결과(실제 로컬 PostgreSQL/PostGIS, TSK-DATA-005 fixture 기반)**: `publicDataId`/`name`/`category`/`categoryName`/`address`/`longitude`/`latitude`/`phone`/`imageUrl`/`createdAt`/`updatedAt` 전부 정상 저장 확인(`TourDataInitialIngestionPipelineTest`). 실제 API를 호출하는 opt-in 통합 테스트(`TourDataInitialIngestionRealApiIntegrationTest`, 기본 스킵)도 동일한 검증을 실제 5건 데이터로 수행하도록 추가(로컬 승인 키 필요, `@Transactional` 자동 롤백으로 DB에 데이터가 남지 않음).

- **3) 반려동물 정책 null 정책 검증**: `TSK-DATA-008`에서 수정한 `TourApiPlaceMapper`/`Place` 생성자가 실제 파이프라인 전체 경로(수집→매핑→저장→DB 재조회)에서도 정상 작동함을 확인 — `maxWeightLimitKg`/`allowedSizes`/`isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone` 전부 DB 저장 후에도 `null`로 남아 있음을 실제 DB 재조회로 검증(단위 테스트가 아니라 실제 JPA/Hibernate 영속화 경로까지 확인한 것이 이번 작업의 핵심 가치). "전체 크기 허용"/`indoor=true`/`outdoor=true`/`offLeash=false` 같은 근거 없는 기본값이 전혀 생성되지 않음을 확인.

- **4) 중복 처리 방식**: 새로운 upsert 시스템을 만들지 않고, 기존에 이미 구현되어 있던 `PlaceRepository.findByPublicDataId()` 기반 방식(신규면 `save()`, 기존이면 `Place.updateDetails()`로 기본정보만 갱신 — 정책 필드는 건드리지 않음, ADR-007 6번 원칙과 이미 일치)을 그대로 재사용. 동일 5건을 두 번 적재해도 `placeRepository.count()`가 첫 실행 이후 변하지 않음을 실제 DB로 검증(`TourDataInitialIngestionPipelineTest#sameBatch_ProcessedTwice_DoesNotDuplicate`).

- **5) 잘못된 데이터 처리**:
  - `contentid` 없음/공백: 기존 `saveCollectedPlaces()`의 `continue` 스킵 로직이 그대로 작동, 해당 항목만 제외되고 나머지는 정상 처리됨을 검증.
  - `mapx`/`mapy` 빈 문자열 또는 파싱 불가: 기존 TSK-DATA-002 안전 파싱 정책(`TourApiPlaceItemDto.getLongitude()/getLatitude()`가 예외 없이 `null` 반환)이 그대로 작동해 배치 전체가 깨지지 않음을 확인.
  - **구조적 제약(신규 발견, 이번 범위에서 해결하지 않음, `TSK-DATA-003-B`에서 해결됨)**: `Place.latitude`/`longitude`/`location` 컬럼은 `V1__init_schema.sql`에서 `NOT NULL`로 정의되어 있어, `TourApiPlaceMapper.toEntity()`는 좌표가 없으면 이 문제를 피하기 위해 `0.0`/`0.0`으로 대체해 저장한다(TSK-DATA-002 때부터 존재하던 기존 동작). 이번 작업이 요구한 "좌표가 없으면 null"은 이 NOT NULL 제약 때문에 **DB migration 없이는 문자 그대로 만족시킬 수 없다** — 이번 범위(migration 금지)에서는 기존 동작을 유지하고 구조적 한계로 기록만 한다(`0.0/0.0`은 임의의 서울 좌표 등 그럴듯한 값은 아니지만 엄밀한 null도 아님). 후속 백로그로 `TSK-DATA-003-B`에 이관 — `TSK-DATA-003-B`에서 migration 없이 "좌표 없는 장소는 적재 제외"로 해결됨(ADR-008).

- **6) 테스트**: 신규 오프라인 테스트 클래스 `TourDataInitialIngestionPipelineTest`(4개, `@SpringBootTest`+`@Transactional`, `TourApiClient`만 Mock 처리하고 나머지는 실제 Spring Bean/실제 로컬 PostgreSQL 사용) — ① 실제 fixture 전체 파이프라인 성공 + 정책 5개 필드 null 검증, ② 동일 배치 재실행 시 미중복 검증, ③ 좌표 결측/파싱 실패 혼재 시 배치 전체가 깨지지 않는지 검증, ④ contentId 없는/공백인 항목이 스킵되고 나머지는 정상 처리되는지 검증. 신규 opt-in 실통합 테스트 `TourDataInitialIngestionRealApiIntegrationTest`(기본 스킵, `-DrunTourApiIntegrationTest=true` 필요) 추가. 전체 `./gradlew test`: **188 tests, 0 failures, 0 errors, 2 skipped**(둘 다 의도된 게이팅 — 기존 `TourApiClientRealApiIntegrationTest` + 신규 테스트). 실제 API 키는 로그/코드/fixture/문서 어디에도 기록하지 않음.

- **후속 백로그**:
  - `TSK-DATA-003-B`(2026-09-14 완료, 아래 참조): 좌표 결측 장소 처리 정책 결정 및 최소 구현.
  - `TSK-DATA-003-C`: 이번에 검증된 최소 파이프라인을 기반으로 한 실제 대량/전국 초기 적재(지역별 반복 호출, 배치 스케줄러 활성화 여부, 속도 제한 대응 등 별도 설계 필요). `TSK-DATA-003-B` 완료 이전에는 착수하지 않는다.

### 2026-09-14 완료 기록: 좌표 결측 장소의 초기 적재 정책 결정 및 최소 구현 (`TSK-DATA-003-B`)
- **범위**: 전국 대량 적재 없음, DB migration 없음, Frontend 변경 없음, 불필요한 Entity/DB 구조 변경 없음. 정책 결정과 최소 구현만 수행.

- **1) 현재 구조 확인 결과**: `Place.latitude`(`NOT NULL, NUMERIC(10,7)`)/`longitude`(`NOT NULL, NUMERIC(10,7)`)/`location`(`NOT NULL, GEOMETRY(Point,4326)`) 3개 컬럼 모두 `V1__init_schema.sql`에서 `NOT NULL`로 정의되어 있음을 재확인. `TourApiPlaceMapper.toEntity()`(수정 전)는 `dto.getLongitude() != null ? dto.getLongitude() : 0.0`(latitude도 동일)으로 좌표 결측 시 `0.0`/`0.0`(기니만 인근 "Null Island")을 대체 저장하고 있었다 — TSK-DATA-002 때부터 존재하던 기존 동작. `PlaceRepositoryImpl`(검색 QueryDSL)과 `PlaceSearchResponseDto`는 좌표가 항상 유효한 값이라는 전제로 작성되어 있어(`ST_DWithin`/`ST_Distance` 등), `null` 좌표를 지원하려면 이 전반을 재검토해야 하는 구조임을 확인.

- **2) 정책 후보 비교 및 결정**: 전략 A(nullable migration 후 좌표 `null` 허용) vs 전략 B(좌표 없는 장소는 Place 적재 자체에서 제외)를 제품 목적/검색 API/지도/거리순 정렬/데이터 보존/향후 좌표 보강/구현 복잡도 기준으로 비교했다. **전략 B를 채택**했다 — 근거: (1) 실제 확인된 `areaBasedList2` 응답은 거의 항상 유효한 좌표를 포함해 결측이 예외적 케이스이므로 검색/지도 전반의 null-safety 재검토 비용이 정당화되지 않음, (2) PetSpot은 위치 기반 매칭이 핵심 가치라 위치 모르는 장소는 서비스 목적에 부합하지 않음, (3) migration 없이 기존 검색/지도 불변식을 그대로 유지할 수 있어 이번 작업의 범위 제한과 일치. 상세 비교표는 `docs/DECISIONS.md`의 **ADR-008** 참조.

- **3) 0.0 fallback 제거**: `TourApiPlaceMapper.toEntity()`에서 좌표 대체 로직을 제거하고, `longitude`/`latitude` 중 하나라도 `null`이면(파싱 실패/빈 문자열 포함) `dto == null`일 때와 동일하게 메서드 전체가 `null`을 반환하도록 변경 — 임의의 좌표를 만들어내지 않는다.

- **4) 적재 스킵 구현**: `TourDataCollectorService.saveCollectedPlaces()`에 `tourApiPlaceMapper.toEntity(dto)`가 `null`을 반환하는 경우(좌표 결측/파싱 실패) `contentId` 결측과 동일하게 해당 항목만 건너뛰고(`skippedCount`로 로그, 반환값 `int[]`의 신규/업데이트 카운트에는 포함되지 않음) 나머지 배치는 계속 진행하도록 수정. 신규 upsert 시스템/큐/재시도 로직은 만들지 않았다(범위 제한 준수) — 좌표가 나중에 보강되면 기존 `publicDataId` 기반 upsert를 재실행하는 것만으로 자동 반영됨.

- **5) 검색/지도 영향 확인**: `PlaceRepositoryImpl`/`PlaceSearchResponseDto`/QueryDSL 쿼리는 전혀 수정하지 않았다 — 좌표 없는 장소가 애초에 DB에 존재하지 않으므로 기존 "좌표는 항상 유효하다" 불변식이 그대로 유지된다. `(0,0)` 근방 반경 검색으로도 좌표 결측 장소가 결과에 나타나지 않음을 테스트로 확인.

- **6) 테스트**: `TourApiPlaceMapperTest`의 기존 `toEntity_BlankCoordinates_DefaultsToZero`/`toEntity_MalformedCoordinates_DefaultsToZero`를 `..._ReturnsNull`로 교체(0.0 기대값 → `null` 반환 기대값), 좌표를 지정하지 않았던 기존 정책/체중 테스트 2개에 유효 좌표를 추가(그렇지 않으면 매퍼가 이제 전체를 `null` 반환하므로). `TourDataInitialIngestionPipelineTest`의 기존 `mixedBatch_...` 테스트를 "좌표 결측 2건은 스킵되고 정상 좌표 1건만 저장됨" + "(0,0) 근방 검색에도 나타나지 않음"으로 갱신. 전체 `./gradlew test`: **188 tests, 0 failures, 0 errors, 2 skipped**(변경 없음 — 테스트를 교체했을 뿐 순증감 없음).

- **7) 최종 정리(문서화 필수 항목)**:
  - 좌표 결측 정책: **전략 B — 좌표 없는 장소는 Place에 저장하지 않음(적재 제외)**.
  - 0.0 fallback: **제거됨**.
  - 좌표 없는 장소의 DB 저장 여부: **저장 안 함**(장소 기본정보 자체가 DB에 남지 않음).
  - 지도 검색 처리 방식: **변경 없음**(좌표 없는 장소가 존재할 수 없으므로 별도 처리 불필요).
  - 거리순 검색 처리 방식: **변경 없음**(동일 이유).
  - 향후 좌표 보강 가능성: TourAPI/PetSpot이 이후 좌표를 확보하면 기존 배치 재실행(`publicDataId` upsert)만으로 자동 반영. 스킵된 장소를 별도로 추적할지는 결정 필요로 남김(현재는 로그로만 확인 가능).

### 2026-08-18 완료 기록: 장소 상세 페이지 즐겨찾기 기능 연동
- **구현 범위**: `PlaceDetailPage.vue`에 로그인 사용자 기준 즐겨찾기 상태 표시, 기존 `GET /users/me/favorites`/`POST·DELETE /favorites/{placeId}` API 재사용, 추가/삭제 토글, 비로그인 시 로그인 화면(`redirect` 쿼리) 이동, 진행 중 버튼 잠금(중복 클릭 방지), 성공 시 즉시 상태 갱신, 실패 시 상태 유지 + `extractErrorMessage`/`ErrorMessage` 패턴으로 오류 표시. 새로운 백엔드 API는 만들지 않고 기존 `FavoriteController`/`FavoriteResponseDto`를 그대로 사용.
- **테스트 결과**:
  - Vitest: 전체 49/49 통과(`PlaceDetailPage.spec.ts` 즐겨찾기 관련 8개 시나리오 — 상태 조회, 추가 성공, 삭제 성공, 비로그인 처리, API 실패 처리, 연속 클릭 방지, 기존 상세 페이지 테스트 포함 — 전부 포함).
  - `vue-tsc --noEmit`(type-check) 및 `npm run build` 통과.
  - 백엔드 `./gradlew test`: 로컬 PostgreSQL 기동 후 전체 통과(`PlaceDetailIntegrationTest` 포함, 실DB 기반 검증).
  - Playwright 실브라우저 검증(임시 시드 장소 1건, 회원가입으로 생성한 임시 계정 사용, 검증 후 시드 데이터 삭제): 회원가입 → 로그인 → 장소 검색 → 장소 상세 → 즐겨찾기 추가("즐겨찾기됨" 표시 확인) → 새로고침(상태 유지 확인) → 즐겨찾기 삭제("즐겨찾기" 상태로 복귀 확인) 전 시나리오 통과.
- **발견된 기술부채**: 단건 즐겨찾기 상태 확인 전용 API 부재(`TSK-BE-030`), `favoriteStore` 미사용으로 인한 기존 스토어 부채 지속(`TSK-FE-015` 갱신).

### 2026-08-18 완료 기록: 장소 상세 페이지 리뷰 조회/작성 기능 구현
- **구현 범위**: `PlaceDetailPage.vue`에 리뷰 목록 조회 및 작성 UI 추가. 백엔드는 변경 없이 기존 `ReviewController`의 `GET /api/v1/places/{placeId}/reviews`(인증 불필요, 목록 조회), `POST /api/v1/reviews`(인증 필요, 작성)를 그대로 재사용. 프런트엔드에 `types/review.ts`(`Review`/`ReviewCreateRequest`, `ReviewResponseDto`/`ReviewCreateRequestDto`와 1:1 매핑) 신규 추가. 리뷰 없음은 기존 `EmptyState` 컴포넌트로 표시, 비로그인 사용자에게는 작성 폼 대신 로그인 안내 + 로그인 이동 버튼 표시, 로그인 사용자는 별점(1~5, 클릭형)/내용(최대 1000자) 입력 후 등록. 등록 성공 시 리뷰 목록(`fetchReviews`)과 장소 상세(`fetchPlaceDetail`)를 동시 재조회해 `rating`/`reviewCount`를 즉시 갱신. 중복 작성(409, 기존 백엔드 정책 그대로), 평점/내용 검증 실패(400), 기타 서버 오류는 모두 기존 `extractErrorMessage`/`ErrorMessage` 패턴으로 표시. 기존 장소 상세 조회·즐겨찾기 로직은 변경하지 않음.
- **테스트 결과**:
  - Vitest: 전체 56/56 통과(`PlaceDetailPage.spec.ts`에 리뷰 관련 7개 시나리오 신규 — 목록 조회, 리뷰 없음(EmptyState), 작성 성공 후 목록/평점 즉시 갱신, 평점·내용 미입력 시 클라이언트 검증, 비로그인 안내 표시, 중복 작성(409) 오류, 기타 API 실패 오류 — 및 기존 즐겨찾기/상세 조회 테스트 전부 회귀 없이 포함).
  - `vue-tsc --noEmit`(type-check) 및 `npm run build` 통과.
  - 백엔드 `./gradlew test`: 변경 없음, 로컬 PostgreSQL 기동 후 전체 통과 재확인(기존 `ReviewControllerTest`/`ReviewServiceTest`/`ReviewRepositoryTest` 포함).
  - Playwright 실브라우저 검증(DB에 임시 시드 장소 1건, 회원가입으로 생성한 임시 계정 사용, 검증 후 리뷰/즐겨찾기/장소/계정 전부 삭제): 로그인 → 장소 검색("E2E" 키워드) → 상세 진입(리뷰 0건 EmptyState 확인) → 리뷰 작성(별점 5, 내용 입력) → 등록 직후 목록·리뷰 1개 카운트 즉시 반영 확인 → 새로고침 후에도 작성한 리뷰와 카운트 유지 확인 → 즐겨찾기 추가("즐겨찾기됨" 표시) 후 새로고침해도 상태 유지 확인(기존 기능 회귀 없음) → 동일 사용자로 재작성 시도 시 중복 작성 오류 메시지("이미 이 장소에 리뷰를 작성하셨습니다.") 노출 확인. 전 시나리오 통과.
- **발견된 기술부채**: 이 저장소에는 Playwright 등 E2E 테스트 하네스가 아직 구성되어 있지 않아(`package.json`에 스크립트 없음), 이번 검증은 임시로 로컬에 Playwright를 설치하고 DB에 임시 시드 데이터를 직접 심어 수행했다(검증 후 전부 원복). 반복 가능한 E2E 검증을 위해 정식 Playwright 설정(`playwright.config.ts`, 테스트 DB/시드 스크립트, npm script)을 별도로 도입할 필요가 있다 — `TSK-QA-002`로 하단 Feature 4.4 백로그에 신규 추가.

### 2026-08-27 완료 기록: 장소 상세 페이지 사진/반려동물 동반 수칙 보강
- **구현 범위**: 백엔드 변경 없음. `PlaceDetailPage.vue`에 두 가지를 보강.
  - **사진**: 기존에는 `imageUrl`이 없을 때만 fallback 아이콘을 표시했고, `imageUrl`이 있지만 실제 로드에 실패하는 경우(깨진 링크 등)는 처리하지 않았다. `<img>`에 `@error` 핸들러를 추가해 로드 실패 시에도 동일한 fallback 아이콘 UI로 전환되도록 수정. 외부 이미지 URL을 새로 생성하거나 이미지 API를 추가하지 않았으며, 현재 API 계약(`imageUrl: string | null`)을 그대로 유지해 향후 한국관광공사 이미지 데이터가 이 필드로 연결되어도 구조 변경이 필요 없다.
  - **반려동물 동반 수칙**: 백엔드 `Place` 엔티티/`PlaceSearchResponseDto`를 먼저 확인한 결과, API로 실제 노출되는 반려동물 관련 필드는 `allowedSizes`, `maxWeightLimitKg` 둘뿐이었다(엔티티에는 `isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone`도 있으나 DTO 미포함 확인, `TSK-BE-031`로 백로그 기록). "반려동물 동반 수칙" 섹션을 신설해 기존 크기 배지 UI(`allowedSizes`)를 그대로 재사용하고, 그동안 응답에는 있었지만 화면에 표시되지 않던 `maxWeightLimitKg`를 "최대 체중 N kg까지 동반 가능" 문구로 추가 표시. 둘 다 없으면 안내 문구만 표시. 새 백엔드 API나 DTO 필드는 추가하지 않았다.
  - 기존 즐겨찾기 토글, 리뷰 조회/작성, 비로그인 조회 흐름은 코드 변경 없이 그대로 유지.
- **테스트 결과**:
  - Vitest: 전체 61/61 통과(`PlaceDetailPage.spec.ts`에 사진 3개 — 정상 표시, imageUrl 없음, 로드 실패 — 및 반려동물 동반 수칙 2개 — 정보 있음, 정보 없음 — 신규 시나리오 포함, 기존 즐겨찾기/리뷰/상세 조회 테스트 전부 회귀 없이 포함).
  - `vue-tsc --noEmit`(type-check) 및 `npm run build` 통과.
  - 백엔드 변경 없어 `./gradlew test`는 별도 실행하지 않음.
  - Playwright 실브라우저 검증은 이번 작업에서는 수행하지 않음(`TSK-QA-002`에 기록된 대로 재사용 가능한 E2E 하네스가 아직 없어 매번 임시 설치/시드가 필요하므로, Vitest 컴포넌트 테스트로 사진 fallback/동반 수칙 표시 로직을 검증하는 것으로 갈음). 재사용 가능한 하네스가 마련되면 `TSK-QA-002` 완료 시 이번 흐름(검색→상세→사진/동반수칙→리뷰→즐겨찾기)도 함께 회귀 테스트로 편입 필요.
- **발견된 기술부채**: `TSK-BE-031`(실내/실외/목줄 자유구역 정책 API 미노출) 신규 기록.

### 2026-08-27 완료 기록: 즐겨찾기 목록 화면 CRUD 연동
- **구현 범위**: 백엔드 변경 없음(신규 API 없음). `FavoritesPage.vue`를 조사 결과 이전에는 `fav.id`(실제 응답 필드는 `favoriteId`) 참조 등 실제 API 응답과 어긋난 스캐폴딩 코드였음을 확인하고, 기존 `types/favorite.ts`(`FavoriteItem`, `FavoriteResponseDto`와 1:1)와 기존 `GET /api/v1/users/me/favorites`/`DELETE /api/v1/favorites/{placeId}`(`PlaceDetailPage.vue`의 즐겨찾기 토글에서 이미 사용 중이던 것과 동일 API)를 그대로 재사용해 전면 재작성.
  - 목록 조회: 로딩/빈 목록(`EmptyState`)/API 오류(`ErrorMessage`, `extractErrorMessage`) 상태 처리, 카드에 장소명/주소/이미지(`imageUrl` 없음 또는 로드 실패 시 fallback 아이콘)/카테고리명/평점·리뷰 수 표시(모두 기존 API 응답 필드).
  - 상세 이동: 카드 클릭 시 `router.push({ name: 'PlaceDetail', params: { id: fav.placeId } })`로 기존 `/place/:id` 라우트 재사용.
  - 삭제: 기존 `DELETE /favorites/{placeId}` 재사용, 카드 클릭 이벤트와 분리(`@click.stop`), `removingPlaceId` 상태로 중복 클릭 방지(진행 중 버튼 비활성화), 성공 시 목록에서 즉시 제거(재조회 없이 클라이언트에서 filter), 실패 시 목록 유지 + 별도 `ErrorMessage`로 오류 표시.
  - 비로그인: `/favorites` 라우트에는 이미 `meta.requiresAuth`가 설정되어 라우터 가드가 우선 차단하지만, 컴포넌트가 라우터 없이 단독으로 마운트되는 경우(테스트 등)에도 안전하도록 `onMounted`에서 `authStore.isAuthenticated`를 확인해 API 호출 없이 로그인 화면(`redirect=/favorites`)으로 이동하는 방어 로직을 추가.
  - `favoriteStore`는 여전히 미사용 — Feature 3.1/`TSK-FE-015`에 이미 기록된 기존 패턴(페이지가 `apiClient`를 직접 호출)을 유지, 이번 작업에서 전역 상태 구조로 무리하게 확장하지 않음.
- **테스트 결과**:
  - Vitest: 전체 70/70 통과(`FavoritesPage.spec.ts` 신규 9개 — 비로그인 리다이렉트, 목록 조회, 로딩, 빈 목록, API 오류, 상세 이동, 삭제 성공, 삭제 실패, 중복 삭제 요청 방지 — 및 기존 테스트 전부 회귀 없이 포함).
  - `vue-tsc --noEmit`(type-check) 및 `npm run build` 통과.
  - 백엔드 변경 없어 `./gradlew test`는 별도 실행하지 않음.
  - Playwright 실브라우저 검증: 이번 환경에는 로컬 PostgreSQL/백엔드 구동 수단이 없어(`TSK-QA-002`에 기록된 대로 재사용 가능한 E2E 하네스도 부재) 수행하지 못함. Vitest 컴포넌트 테스트로 목록 조회/상세 이동/삭제 흐름을 대체 검증.
- **발견된 기술부채**: 기존 `FavoritesPage.vue`가 실제 API 응답 필드(`favoriteId`)와 다른 `fav.id`를 참조하던 버그를 이번 재작성 과정에서 발견 및 수정(별도 Task로 분리할 실익이 없어 이번 구현에 포함해 수정함, 범위 확장 아님 — 화면이 사실상 비어 있던 필드 표시 버그).

### 2026-08-27 완료 기록: 반려동물 관리 화면 CRUD 연동
- **구현 범위**: 백엔드 변경 없음(`PetController`/`PetService`/`PetRepository`/DTO 조사 결과 등록·목록·수정·삭제·대표 지정 API가 이미 모두 구현되어 있음을 확인, 신규 API 없이 그대로 재사용). `PetPage.vue`를 실제 API 계약 기준으로 전면 재작성.
  - **타입 불일치 수정**: 기존 `types/user.ts`의 `Pet` 인터페이스가 실제 `PetResponseDto`에 없는 `species`/`userId`/`ageYears`/`isVaccinated` 필드를 포함하고, 실제로 존재하는 `gender`/`birthDate`/`neutered`는 누락되어 있던 것을 확인. 이 타입을 사용하는 곳은 `PetPage.vue`(재작성 대상)와 `SearchPage.vue`(`name`/`sizeCategory`/`representative`만 사용, 영향 없음)뿐임을 먼저 확인한 뒤, `Place`/`PetPolicy`(`TSK-FE-014`) 때와 달리 이번에는 실사용처가 한정적이고 명확한 버그였으므로 별도 병행 타입을 만드는 대신 `Pet` 인터페이스 자체를 실제 응답과 일치하도록 직접 수정(최소 수정 원칙). `PetCreateRequest`/`PetUpdateRequest` 타입 신규 추가(`PetCreateRequestDto`/`PetUpdateRequestDto`와 1:1).
  - **목록 조회**: `GET /pets` 재사용, 로딩/빈 목록(`EmptyState`)/API 오류(`ErrorMessage`) 처리, 카드에 이름/품종/성별/생년월일/체중/중성화 여부/크기 카테고리/대표 여부 배지 표시.
  - **등록/수정**: 기존 `BaseModal`/`BaseInput`/`BaseButton` 공용 컴포넌트를 재사용한 단일 폼으로 등록(`POST /pets`)과 수정(`PUT /pets/{petId}`)을 함께 처리. 이름/품종/체중/성별 미입력 또는 체중 0 이하 시 클라이언트 검증 메시지 표시 후 API 미호출(서버 필수 검증과 동일한 범위), 서버 400 등 실패 시 `extractErrorMessage`/`ErrorMessage`로 표시, 요청 중 버튼 잠금. `sizeCategory`는 입력값이 아니라 응답으로만 받아 표시 — 프론트에 체중 구간 판정 로직을 두지 않음(백엔드 `PetSizeCategory.fromWeight` 결과 그대로 신뢰).
  - **삭제**: `BaseModal` 기반 확인 다이얼로그(장소/펫 이름 표시) 도입 후 확인 시에만 `DELETE /pets/{petId}` 호출, 요청 중 버튼 잠금, 성공 시 목록 재조회(백엔드가 삭제된 펫이 대표였을 경우 자동으로 다른 펫을 대표로 재지정하는 정책을 그대로 반영하기 위해 재조회 방식 사용).
  - **대표 반려동물 지정**: 기존 `PATCH /pets/{petId}/representative` 재사용, 이미 대표인 펫은 버튼 미노출, 변경 중 요청 잠금, 성공 시 목록 재조회로 최신 대표 상태 반영, 실패 시 별도 `ErrorMessage` 표시.
  - **기존 구조 보호**: `SearchPage.vue`의 대표 반려동물 기반 크기 필터 기본값 로직(`GET /pets` 자체 호출, `Pet` 타입의 `name`/`sizeCategory`/`representative` 사용)은 이번 타입 수정 후에도 그대로 동작함을 코드 검토로 확인, 변경하지 않음. `usePetStore`는 기존과 마찬가지로 미사용 상태 유지(`TSK-FE-015` 기존 기술부채 그대로, 이번 작업에서 무리하게 전역 상태로 확장하지 않음).
- **테스트 결과**:
  - Vitest: 전체 81/81 통과(`PetPage.spec.ts` 신규 11개 — 비로그인 리다이렉트, 목록 조회, 빈 목록, API 오류, 등록 검증 실패, 등록 성공, 수정 성공, 삭제 성공, 대표 변경 성공, 대표 변경 실패, 중복 삭제 요청 방지 — 및 기존 테스트 전부 회귀 없이 포함).
  - `vue-tsc --noEmit`(type-check) 및 `npm run build` 통과.
  - 백엔드 변경 없어 `./gradlew test`는 별도 실행하지 않음.
  - Playwright 실브라우저 검증: 이번 환경에는 로컬 PostgreSQL/백엔드 구동 수단이 없어(`TSK-QA-002`) 수행하지 못함. Vitest 컴포넌트 테스트로 등록/수정/삭제/대표 지정 흐름을 대체 검증.
- **발견된 기술부채**: 없음(기존 `TSK-FE-015`, `TSK-QA-002`에 계속 해당). `types/user.ts`의 `Pet` 타입 불일치는 이번 작업에서 직접 수정 완료.

### 2026-08-27 완료 기록: 마이페이지 통합 화면 연동
- **구현 범위**: 백엔드 변경 없음(`UserController`/`MyPageQueryService`/`UserProfileService` 조사 결과 필요한 API가 이미 모두 존재함을 확인, 신규 API 없이 그대로 재사용). 문서상 Sprint 2에 "미완료"로 남아 있던 `TSK-BE-020`(마이페이지 요약 API)/`TSK-BE-021`(대시보드 API)이 실제로는 이미 구현·연동되어 있었음을 이번 조사에서 확인하고 문서를 실제 상태로 갱신(코드 변경 아님).
  - **타입 신규 추가**: `types/user.ts`에 `MyPageSummary`/`MyPageRepresentativePet`(`MyPageResponseDto`/`RepresentativePetDto`와 1:1), `UserProfileUpdateRequest`/`UserProfileResponse`(`UserProfileUpdateRequestDto`/`UserProfileResponseDto`와 1:1) 신규 추가. 기존 `Pet` 타입과 필드가 겹치지만 실제로는 다른 응답 구조(대표 반려동물 요약은 필드가 더 적음)라 별도 타입으로 분리, 실제 응답에 없는 필드는 추가하지 않음.
  - **MyPage.vue**: `GET /users/me` 재사용, 로딩/오류(`ErrorMessage`)/데이터 표시 상태 처리(빈 목록 개념은 없음 — 사용자 본인 정보이므로 항상 존재), 이메일/닉네임/프로필 이미지(있을 때만 `<img>`, 없으면 이니셜) 표시. 대표 반려동물은 `representativePet`이 있으면 이름/품종/체중/크기(기존 `sizeLabel` 배지 패턴 재사용)를, 없으면 `EmptyState`로 안내. 즐겨찾기/반려동물 통계 버튼 및 하단 메뉴 카드 클릭 시 각각 기존 `/favorites`(`Favorites`)/`/pets`(`Pets`) 라우트로 이동 — 화면을 중복 구현하지 않음. 비로그인 시 API 호출 없이 로그인 화면(`redirect=/mypage`)으로 이동.
  - **회원 정보 수정**: 기존 `BaseModal`/`BaseInput` 재사용, 닉네임(2자 이상, 서버 검증과 동일 범위 클라이언트 검증) + 프로필 이미지 URL(선택) 입력 후 `PUT /users/me` 호출. 성공 시 `authStore.setUser()`로 상단 헤더 닉네임도 즉시 동기화, 요청 중 버튼 잠금, 실패 시 `ErrorMessage` 표시.
  - **대표 반려동물 최신 상태 반영 확인**: `MyPage.vue`가 캐시나 store 없이 마운트 시점마다 `GET /users/me`를 새로 호출하는 구조이므로, `PetPage`에서 대표 반려동물을 변경한 뒤 `/mypage`로 이동하면 별도 동기화 로직 없이도 항상 최신 값이 반영됨을 코드 구조로 확인(수동 캐시 무효화 불필요).
  - **MyReviewsPage.vue 신규 추가(`/mypage/reviews`)**: "리뷰" 메뉴가 이동할 기존 화면이 없었으나, 백엔드에 `GET /api/v1/users/me/reviews`(`ReviewController`, 기존 API)가 이미 구현되어 있음을 확인. 최소 범위(목록 조회, 로딩/빈 목록/오류 상태, 카드 클릭 시 `/place/:id` 이동)의 읽기 전용 화면만 신규 구현 — 수정/삭제 UI는 이번 범위에 포함하지 않음(요청 범위 밖).
- **테스트 결과**:
  - Vitest: 전체 94/94 통과(`MyPage.spec.ts` 신규 8개 — 비로그인 리다이렉트, 정보/대표펫 표시, 대표펫 없음, API 오류, 즐겨찾기/리뷰/반려동물 메뉴 이동, 정보 수정 성공 — 및 `MyReviewsPage.spec.ts` 신규 5개 — 비로그인 리다이렉트, 목록 조회, 빈 목록, API 오류, 상세 이동 — 포함, 기존 테스트 전부 회귀 없이 포함).
  - `vue-tsc --noEmit`(type-check) 및 `npm run build` 통과.
  - 백엔드 변경 없어 `./gradlew test`는 별도 실행하지 않음.
  - Playwright 실브라우저 검증: 이번 환경에는 로컬 PostgreSQL/백엔드 구동 수단이 없어(`TSK-QA-002`) 수행하지 못함. Vitest 컴포넌트 테스트로 정보 표시/메뉴 이동/정보 수정 흐름을 대체 검증.
- **발견된 기술부채**: `TSK-FE-016`(`types/user.ts`의 `User` 타입이 실제 로그인 응답과 불일치) 신규 기록. `TSK-BE-020`/`TSK-BE-021`은 이미 구현되어 있었음을 확인해 문서만 갱신(코드 변경 아님).

### 2026-08-27 완료 기록: Kakao Map SDK 로딩/렌더링 기반 (0단계)
- **구현 범위**: 백엔드 변경 없음. `SearchPage.vue`는 이번 작업에서 전혀 건드리지 않음(요청대로). 장소 API 연동/마커 표시/검색 결과 연동은 이번 범위에서 명시적으로 제외 — 직전에 한 차례 구현했던 마커 연동(`4a4b00c`)은 이번 요청에 따라 되돌리고 SDK 로딩/렌더링만 남겼다(`TSK-FE-017`로 재기록, git 히스토리에 참고 구현 존재).
  - **Kakao SDK 연동 구조**: `src/utils/kakaoMap.ts`(`loadKakaoMaps()`). `VITE_KAKAO_MAP_APP_KEY` 환경변수로 키를 관리하며 소스에 키를 하드코딩하지 않음. `frontend/.env.development`/`.env.production`에는 빈 값 플레이스홀더만 커밋, 실제 키는 `frontend/.env.local`(`.gitignore`의 `frontend/*.local` 패턴으로 이미 추적 제외, Vite가 자동 로드해 커밋된 값을 덮어씀)에 개발자가 개별 설정. 스크립트 태그를 동적으로 삽입해 SDK를 로드하고, 키 미설정/스크립트 로드 실패/이미 로드됨(중복 삽입 방지) 세 경로를 모두 reject/resolve로 구분.
  - **Kakao 콘솔 Web 플랫폼 도메인(`http://localtest.me:3000`) 대응**: `frontend/vite.config.ts`의 dev 서버가 기본 설정(`host` 미지정)에서는 이 환경 기준 IPv6 루프백(`::1`)에만 바인딩되어 `127.0.0.1` 기반 호스트명(→ `localtest.me`)이 연결 거부되는 것을 확인, `server.host: '127.0.0.1'`을 명시해 해결. Vite 5의 Host 헤더 검증(`allowedHosts`)에 `localtest.me`를 추가해 해당 도메인으로 dev 서버 접근이 차단되지 않도록 구성. 로컬에서 `curl http://localtest.me:3000/map` → 200 확인.
  - **`/map` 라우트 및 `MapPage.vue`**: 라우터에 `/map`(`Map`) 신규 등록, 로그인 불필요. `MainLayout.vue` 상단 네비게이션에 "지도" 링크 추가.
  - **현재 위치/기본 위치**: 마운트 시 `navigator.geolocation.getCurrentPosition`으로 현재 위치를 요청(브라우저 미지원/권한 거부 시 에러를 던지지 않고 안내 메시지만 표시). 좌표를 얻으면 그 위치를, 실패하면 기본 중심(서울시청, 37.5665/126.9780)을 `kakao.maps.Map`의 초기 `center`로 사용해 지도를 렌더링.
  - **오류 처리**: `loadKakaoMaps()` reject 시(키 미설정/스크립트 로드 실패) `ErrorMessage`로 표시하고 지도 컨테이너 자체를 렌더링하지 않음.
  - **언마운트 정리**: SDK 로드/위치 요청이 끝나기 전에 컴포넌트가 언마운트되어도 이후 콜백이 상태를 갱신하지 않도록 `isUnmounted` 가드를 두고, `onUnmounted`에서 보유 중이던 `mapInstance`/`kakaoSdk` 참조를 정리(Kakao Maps는 별도 destroy API를 제공하지 않아 참조 해제가 정리의 전부).
  - **기존 기능 영향 없음**: `SearchPage.vue`/`PlaceDetailPage.vue`/`FavoritesPage.vue`/`PetPage.vue`/`MyPage.vue` 코드 변경 없음.
- **테스트 결과**:
  - Vitest: 전체 105/105 통과(`kakaoMap.spec.ts` 4개 — API 키 미설정/로드 성공/로드 실패/중복 로드 방지 — 및 `MapPage.spec.ts` 7개 — 로딩 상태→SDK 로드, 지도 초기화, SDK 로드 실패, 현재 위치 반영, 위치 거부 시 기본 좌표, 위치 미지원 시 기본 좌표, 로드 중 언마운트 안전성 — 포함, 기존 테스트 전부 회귀 없이 포함). Kakao SDK는 `vi.mock('@/utils/kakaoMap')`으로 완전히 모킹.
  - `vue-tsc --noEmit`(type-check) 및 `npm run build` 통과.
  - 백엔드 변경 없어 `./gradlew test`는 별도 실행하지 않음.
  - **실브라우저 검증(부분)**: 로컬 dev 서버를 `http://localtest.me:3000/map`으로 기동해 200 응답(정상 라우팅) 확인. 다만 이 환경에는 실제 Kakao 개발 키가 없어(`.env.local` 미설정) 지도가 실제로 렌더링되는 시각적 확인까지는 수행하지 못했다 — 키 미설정 시의 오류 메시지 경로(요구사항 중 하나)는 이 상태에서 그대로 재현/확인됨. 실제 키를 발급받아 `frontend/.env.local`에 설정한 뒤 브라우저에서 지도 렌더링 자체를 육안으로 확인하는 절차가 필요.
- **발견된 기술부채**: 없음(신규 발견 없음). `TSK-FE-017`을 "지도 뷰포트 재검색"에서 "지도에 장소 마커 연동"(마커 표시 자체 + 뷰포트 재검색)으로 범위를 넓혀 재기록.
