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
- [ ] `TSK-FE-006` [P0] Place Search View (`/places`): Keyword Search, Category Filter, Pet Size Condition, Distance Sort
  - 키워드/카테고리 검색 연동 완료. **잔여**: 반려동물 크기 조건 필터, 거리순 정렬 미구현.
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
