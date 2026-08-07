# Sprint Planning Specification (SPRINTS.md)

이 문서는 `SYSTEM_DESIGN.md`, `docs/ROADMAP.md` 및 `docs/TASKS.md`를 바탕으로 **Sprint 0부터 Sprint 4까지**의 애자일 스프린트 실행 계획을 수립한 마스터 명세서입니다 (v2.0).

---

## 📝 Change Log (변경 이력)

- **v2.0 (2026-08-07)**: **실서비스 MVP 우선구축을 위한 스프린트 로드맵 전면 재구성**
  - **Sprint 2 (Backend Feature Completion)**: 백엔드 완결 (Review Service & API, My Page API, Dashboard API, Integration Test & Swagger OpenAPI 완성). 기술부채(ST_DWithin, Pagination, QueryDSL 최적화)는 별도 리팩토링 스프린트로 분리.
  - **Sprint 3 (Frontend MVP)**: Vue 3 + Vite, Pinia, Axios, TailwindCSS 기반 Frontend MVP 구축 (로그인, 회원가입, 홈, 장소 검색, 지도, 상세, 즐겨찾기, 펫 CRUD, 마이페이지 9개 화면 및 백엔드 완전 연동).
  - **Sprint 4 (Advanced AI & Infra)**: AI 추천 시스템, Redis Cache, Elasticsearch, Docker, AWS CI/CD 및 모니터링 구축.
- **v1.1 (2026-08-05)**: Mock-First 개발 및 스마트 매칭 전진 배치.
- **v1.0 (2026-08-04)**: 최초 스프린트 0~3 애자일 계획 수립.

---

## 📌 Sprint 운영 규칙 및 DoD 표준
- **스프린트 주기**: 1~2주 단위
- **Sprint 공통 Definition of Done (DoD)**:
  - 배정된 모든 Task의 DoD 100% 달성 (`docs/TASKS.md` 체크 완료).
  - 백엔드: `./gradlew test` 오류 없이 100% Pass.
  - 프론트엔드: `npm run build` / 타입 검사 Clean Pass.
  - 완료 시 Git Commit & Push 작성.

---

## 🗺️ Sprint Overview Dashboard (v2.0)

| Sprint | Target Scope | Key Deliverables & Goals | Status |
| :--- | :---: | :--- | :---: |
| **Sprint 0** | Infrastructure | 개발 환경 구축, DB/PostGIS 초기화 & 프로젝트 스캐폴딩 | **[COMPLETED]** |
| **Sprint 1** | Backend Core | 공공데이터 수집 파이프라인, PostGIS 위치 검색 API & 기초 모델 | **[COMPLETED]** |
| **Sprint 2** | Backend Complete | **Review Service/API, My Page API, Dashboard API, E2E Integration Test & Swagger** | **[IN_PROGRESS]** |
| **Sprint 3** | Frontend MVP | **Vue 3 + Vite Frontend 9개 화면 구현 & Spring Boot 백엔드 API 실서비스 완전 연동** | **[NEXT]** |
| **Sprint 4** | Advanced AI & Infra | OpenAI AI 추천, Redis Caching, Elasticsearch, AWS & Docker CI/CD | [PLANNED] |

---

## 🚀 Sprint 2: Backend Feature Completion (백엔드 핵심 기능 완결) [IN_PROGRESS]

- **Sprint Goal**: 백엔드 도메인 및 API 레이어 완결 (Review, My Page, Dashboard, Integration Test, Swagger 정리).

### 배정된 핵심 파트
1. **Review Service 및 API 완료**:
   - Review CUD API (`POST`, `GET`, `PUT`, `DELETE /api/v1/reviews`)
   - Review 작성/수정/삭제 시 `Place` 평균 평점 (`rating`) 및 리뷰 수 (`reviewCount`) 실시간 동기화
   - 장소당 1인 1리뷰 409 Conflict 처리 및 Soft Delete 적용
2. **My Page API**:
   - 사용자 정보, 대표 반려동물, 즐겨찾기 목록, 내 리뷰 목록, 내 반려동물 목록 일괄 조회 API (`GET /api/v1/users/me/profile-summary`)
3. **Dashboard API**:
   - 홈 화면용 추천 장소, 인기 장소, 최근 등록 장소, 대표 반려동물 통합 조회 API (`GET /api/v1/dashboard`)
4. **Integration Test (통합 테스트)**:
   - 회원가입 ➔ 로그인 ➔ JWT 발급 ➔ Pet 등록 ➔ Favorite 등록 ➔ Review 작성 ➔ Place Rating 변경 ➔ 조회 전체 Flow 검증 E2E Test.
5. **API Documentation**:
   - Swagger OpenAPI 3.0 명세 정리, API Tag 분류, Request/Response/Error Example 작성.

> [!NOTE]
> **Refactoring Sprint (Sprint 2 종료 후 진행)**:
> `ST_DWithin` 적용, Pagination 최적화, QueryDSL 개선, Domain/Application Layer 정제, Exception 구조 개편, Performance Optimization.

---

## 🚀 Sprint 3: Frontend MVP (Vue 3 프론트엔드 실서비스 구축) [NEXT]

- **Sprint Goal**: Vue 3 + Vite + TailwindCSS 기반 프론트엔드 구축 및 Spring Boot 백엔드 API와의 100% 완전한 연동을 통해 실사용 가능한 MVP 서비스 완성.
- **기술 스택**: Vue 3 (Composition API), Vite, Pinia, Vue Router 4, Axios, TailwindCSS, Spring Boot API 연동, JWT Auth, 반응형 UI.

### Frontend 설계 문서 수립 항목
- Frontend Architecture Spec
- Component Tree Taxonomy
- API Endpoint Mapping Table
- Vue Router 4 Routing Specification
- Pinia Global Store State Architecture
- Axios Interceptor & Auth Token Manager
- Common Response & Screen Layout System

### 9개 구현 화면 명세
1. **로그인 (`/login`)**: JWT 저장, LocalStorage 관리, 자동 로그인, 로그아웃
2. **회원가입 (`/signup`)**: Form Validation, 이메일 중복 확인
3. **홈 (`/`)**: 추천 장소, 인기 장소, 최근 등록 장소, 대표 반려동물 카드 렌더링
4. **장소 검색 (`/places`)**: 키워드 검색, 카테고리 필터, 반려동물 크기 조건, 거리순 정렬
5. **지도 (`/map`)**: Kakao Map / Leaflet 지도연동, 현재 위치 마커, 장소 상세 모달/페이지 이동
6. **장소 상세 (`/places/{id}`)**: 사진, 기본 정보, 리뷰 목록, 즐겨찾기 토글, 리뷰 작성 폼
7. **즐겨찾기 (`/favorites`)**: 즐겨찾기한 장소 목록 및 삭제
8. **내 반려동물 (`/pets`)**: 반려동물 CRUD 및 대표 반려동물 스위칭 설정
9. **마이페이지 (`/mypage`)**: 회원 정보, 즐겨찾기 탭, 내 리뷰 탭, 내 반려동물 탭

---

## 🚀 Sprint 4: Advanced Features, AI & Infra (고급 기능 & 인프라) [PLANNED]

- **Sprint Goal**: AI 추천 알고리즘 및 캐싱/인프라 고도화.
- **주요 구현 항목**:
  - OpenAI API 기반 맞춤형 AI 장소 추천 파이프라인
  - Redis Caching (장소 검색 및 JWT 세션)
  - Elasticsearch 전문 검색 도입
  - Docker Compose Production 환경, AWS 배포 및 CI/CD 파이프라인
