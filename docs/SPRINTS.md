# Sprint Planning Specification (SPRINTS.md)

이 문서는 `SYSTEM_DESIGN.md`, `docs/ROADMAP.md` 및 `docs/TASKS.md`를 바탕으로 **Sprint 0부터 Sprint 3까지**의 애자일 스프린트 실행 계획을 수립한 명세서입니다.

---

## 📝 Change Log (변경 이력)

- **v1.1 (2026-08-05)**: **Agile Coach & Senior Technical PM 리뷰 결과 반영**
  - **Mock-First 백엔드/프론트엔드 병렬 개발**: Sprint 1 시작 시 `TSK-BE-008M` (Mock REST API) 추가하여 FE 디커플링 보장.
  - **MVP 핵심가치 전진 배치**: 펫 매칭 순수 알고리즘(`TSK-BE-012`, `TSK-FE-013`)을 Sprint 1로 전진 배치하여 비회원 상태에서도 스마트 뱃지 UX 빠른 검증.
  - **Sprint 2 과부하 해소**: 17h/12개 Task에서 13.5h/9개 Task로 조정 (회원/펫 프로필 CRUD 집중).
  - **Sprint 3 벨로시티 균형 & 백엔드 유휴 방지**: 백엔드 북마크 API (`TSK-BE-015`) 신설 및 Place Detail Modal (`TSK-FE-015`) 이관으로 FE/BE 공수 균형 최적화.
- **v1.0 (2026-08-04)**: 최초 스프린트 0~3 애자일 계획 수립.

---

## 📌 Sprint 운영 규칙 및 DoD 표준
- **스프린트 주기**: 1~2주 단위 (Sprint 0은 1주, Sprint 1~3은 각 2주)
- **Sprint 0 법칙**: 개발 환경 구축, 인프라/DB 스키마 초기화, 프로젝트 스캐폴딩만 전담 배정.
- **Task Dependency 준수**: 각 Sprint 내 배정된 작업은 선행 Task ID(Dependencies) 순서에 맞춰 수행.
- **Sprint 공통 Definition of Done (DoD)**:
  - 배정된 모든 Task의 DoD 100% 달성 (`docs/TASKS.md` 체크 완료).
  - 백엔드: `./gradlew test` 오류 없이 100% Pass.
  - 프론트엔드: `npm run type-check` (`vue-tsc`) strict typing Clean Pass.
  - 각 Task 완료 시 `docs/SESSION_TEMPLATE.md` 인수인계 리포트 작성.

---

## 🗺️ Sprint Overview Dashboard (v1.1 최적화)

| Sprint | Period | Sprint Goal | Allocated Tasks | Status | Effort |
| :--- | :---: | :--- | :---: | :---: | :---: |
| **Sprint 0** | 1주 | 개발 환경 구축, DB/PostGIS 초기화 & 백엔드/프론트엔드 베이스 스캐폴딩 | 9 Tasks | **[COMPLETED]** | 11h |
| **Sprint 1** | 2주 | Mock API & 공공데이터 수집 배치 가동, PostGIS 반경 검색 API, Leaflet 지도 탐색 UI & **비회원 스마트 매칭 검증** | 11 Tasks | **[READY]** | 16.5h |
| **Sprint 2** | 2주 | **회원 인증 & 펫 프로필 CRUD API**, 대표 펫 전환 연동 및 스마트 매칭 뱃지 실시간 리렌더링 | 9 Tasks | [PENDING] | 13.5h |
| **Sprint 3** | 2주 | **이용후기/북마크 API**, 상세 모달, 모바일 반응형 UI (Bottom Nav & Drawer) 및 PostGIS Latency (P95 < 50ms) / E2E 검증 | 9 Tasks | [PENDING] | 12.5h |
| **합계** | **7주** | **PetSpot MVP 서비스 정식 완성** | **38 Tasks** | **Sprint 0 완료** | **53.5h** |

---

## 🚀 Sprint 0: Setup & Infrastructure (개발 환경 & 스캐폴딩) [COMPLETED]

- **기간**: 1주 (1-Week)
- **Sprint Goal**: PostgreSQL + PostGIS 컨테이너 가동, Flyway 마이그레이션 적용, Spring Boot 3 Security/JWT 베이스 및 Vue 3 + TypeScript Pinia/Router 스캐폴딩 완성.

### 배정된 Tasks (9 Tasks - 100% COMPLETED)

| Task ID | Task Name | Priority | Category | Dependencies | Status | Est. Time |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| `TSK-INF-001` | PostgreSQL 15 + PostGIS Docker Setup | P0 | Infra | None | [x] COMPLETED | 1h |
| `TSK-INF-002` | Flyway DB Migration Scripts | P0 | Infra | `TSK-INF-001` | [x] COMPLETED | 1.5h |
| `TSK-BE-001` | Spring Boot 3.2 Project Scaffold | P0 | Backend | None | [x] COMPLETED | 1h |
| `TSK-BE-002` | SecurityConfig & JWT Provider Implementation | P0 | Backend | `TSK-BE-001` | [x] COMPLETED | 2h |
| `TSK-BE-003` | Global Exception Handler & ApiResponse Wrapper | P0 | Backend | `TSK-BE-001` | [x] COMPLETED | 1h |
| `TSK-FE-001` | Vue 3 + Vite + TypeScript Project Scaffold | P0 | Frontend | None | [x] COMPLETED | 1h |
| `TSK-FE-002` | TypeScript Core Interfaces Setup | P0 | Frontend | `TSK-FE-001` | [x] COMPLETED | 1h |
| `TSK-FE-003` | Pinia & Vue Router 4 Setup | P0 | Frontend | `TSK-FE-002` | [x] COMPLETED | 1.5h |
| `TSK-FE-004` | Design System CSS Tokens & Font Setup | P1 | Frontend | `TSK-FE-001` | [x] COMPLETED | 1h |

### Sprint 0 Definition of Done (DoD)
- [x] Docker 환경에서 PostgreSQL 15 `CREATE EXTENSION postgis;` 적용 완료 (`docker-compose.yml`, `init-postgis.sql`).
- [x] Flyway `V1__init_schema.sql` 스크립트에 의해 `users`, `pets`, `places`, `place_reviews` 테이블 및 Spatial Index `idx_places_location` 작성 완료.
- [x] Spring Boot 3 SecurityConfig & JwtTokenProvider, ApiResponse Wrapper 구축 완료.
- [x] Vue 3 + TypeScript Vite 프로젝트, Pinia Store, Interfaces 및 Design System Tokens `variables.css` 설정 완료.
- [x] 세션 인수인계 리포트 [`docs/sessions/SESSION_SPRINT_0.md`](sessions/SESSION_SPRINT_0.md) 작성 완료.

---

## 🚀 Sprint 1: Core Data Pipeline, Discovery & Early Matching (공공데이터 & 스마트 매칭 검증) [READY]

- **기간**: 2주 (2-Weeks)
- **Sprint Goal**: Mock API를 통한 FE/BE 병렬 개발 환경 확보, 한국관광공사 TourAPI 수집 배치 파이프라인 가동, QueryDSL PostGIS 위치 반경 검색 API, Leaflet 마커 지도 탐색 UI 및 **비회원 상태에서의 스마트 매칭 알고리즘 1차 검증** 완성.

### 배정된 Tasks (11 Tasks)

| Task ID | Task Name | Priority | Category | Dependencies | Est. Time |
| :--- | :--- | :---: | :---: | :---: | :---: |
| `TSK-BE-008M` | Place Mock REST Controller Endpoint | P0 | Backend | `TSK-BE-003` | 1h |
| `TSK-BE-004` | TourAPI Client & Public Data Parser | P0 | Backend | `TSK-BE-001` | 2h |
| `TSK-BE-005` | Public Data Scheduled Ingestion Batch (`@Scheduled`) | P0 | Backend | `TSK-BE-004`, `TSK-INF-002` | 2h |
| `TSK-BE-006` | Place JPA Entity & Spatial Repository | P0 | Backend | `TSK-INF-002`, `TSK-BE-001` | 1.5h |
| `TSK-BE-007` | QueryDSL Spatial Radius & Multi-Filter Query | P0 | Backend | `TSK-BE-006` | 2h |
| `TSK-BE-008` | Place Search REST Controller Endpoints | P0 | Backend | `TSK-BE-007` | 1.5h |
| `TSK-BE-012` | Smart Pet-Matching Service Evaluator Logic | P0 | Backend | `TSK-BE-006` | 1.5h |
| `TSK-FE-005` | Leaflet Map Component (`MapContainer.vue`) | P0 | Frontend | `TSK-FE-003` | 2h |
| `TSK-FE-006` | Search Box & Category Filter Pills (`FilterBar.vue`) | P0 | Frontend | `TSK-FE-003` | 1.5h |
| `TSK-FE-007` | Sub-Filter Tag Component (`SubFilterTags.vue`) | P1 | Frontend | `TSK-FE-006` | 1h |
| `TSK-FE-013` | Client-Side Match Evaluator Helper (`evaluatePetMatch.ts`) | P0 | Frontend | `TSK-FE-002` | 1h |

### Sprint 1 Definition of Done (DoD)
- [ ] `TSK-BE-008M` Mock API 제공으로 프론트엔드가 백엔드 배치 완성을 기다리지 않고 즉시 개발 진행.
- [ ] 배치 작업에 의해 공공데이터 장소 정보가 DB `places` 테이블 및 PostGIS Point 위치 데이터로 정상 저장됨.
- [ ] `GET /api/v1/places` 엔드포인트에서 위치 반경, 카테고리, 견종 크기 다중 필터링 검색 동작.
- [ ] 비회원 디폴트 펫(4.2kg 소형견) 기준 스마트 매칭 헬퍼(`evaluatePetMatch.ts`) 정상 검증.

---

## 🚀 Sprint 2: User Auth & Pet Profile CRUD (회원/펫 프로필 관리 & 실시간 뱃지) [PENDING]

- **기간**: 2주 (2-Weeks)
- **Sprint Goal**: 회원가입/로그인 JWT 인증, 펫 프로필 CRUD API, 대표 펫 전환 연동 및 스마트 매칭 뱃지(`PASS`/`WARN`/`DENY`) 카드 실시간 리렌더링 완성.

### 배정된 Tasks (9 Tasks)

| Task ID | Task Name | Priority | Category | Dependencies | Est. Time |
| :--- | :--- | :---: | :---: | :---: | :---: |
| `TSK-BE-009` | User Entity & Auth REST Controller | P0 | Backend | `TSK-BE-002` | 1.5h |
| `TSK-BE-010` | Pet Entity & Size Category Auto-Calculator | P0 | Backend | `TSK-BE-009` | 1.5h |
| `TSK-BE-011` | Pet Controller REST Endpoints | P0 | Backend | `TSK-BE-010` | 1.5h |
| `TSK-BE-013` | Place Match Evaluation REST Endpoint | P0 | Backend | `TSK-BE-012`, `TSK-BE-008` | 1h |
| `TSK-FE-008` | Place Card & List Component (`PlaceCard.vue`) | P0 | Frontend | `TSK-FE-005`, `TSK-FE-006` | 2h |
| `TSK-FE-009` | User Auth Modal (`AuthModal.vue`) | P0 | Frontend | `TSK-FE-003` | 1.5h |
| `TSK-FE-010` | Pet Grid & Selection Component (`PetGrid.vue`) | P0 | Frontend | `TSK-FE-009` | 1.5h |
| `TSK-FE-011` | Add Pet Form Modal (`AddPetModal.vue`) | P0 | Frontend | `TSK-FE-010` | 1.5h |
| `TSK-FE-012` | Active Pet Header Chip (`ActivePetChip.vue`) | P0 | Frontend | `TSK-FE-010` | 1h |
| `TSK-FE-014` | Smart Match Badge Component (`SmartMatchTag.vue`) | P0 | Frontend | `TSK-FE-013`, `TSK-FE-008` | 1.5h |

### Sprint 2 Definition of Done (DoD)
- [ ] 신규 회원가입 및 로그인 후 JWT 토큰 발급/인증 정상 작동.
- [ ] 펫 등록 시 체중에 따른 소형/중형/대형견 크기 카테고리 자동 산출 및 다중 펫 등록 보존.
- [ ] 대표 펫 전환 시 카드 내 매칭 뱃지(`PASS`/`WARN`/`DENY`)가 100% 실시간 리렌더링됨.

---

## 🚀 Sprint 3: Social Features, Detail Modal, Mobile Responsive UX & QA (상세 모달, 리뷰, 모바일 UI & 품질 검증) [PENDING]

- **기간**: 2주 (2-Weeks)
- **Sprint Goal**: 리뷰 및 북마크 API, 장소 상세 모달, 900px 모바일 Bottom Navigation Bar/Sheet Drawer 레이아웃 완성 및 PostGIS NFR Latency (P95 < 50ms) / E2E 품질 검증 완료.

### 배정된 Tasks (9 Tasks)

| Task ID | Task Name | Priority | Category | Dependencies | Est. Time |
| :--- | :--- | :---: | :---: | :---: | :---: |
| `TSK-BE-014` | PlaceReview Entity & REST Endpoints | P1 | Backend | `TSK-BE-008`, `TSK-BE-009` | 1.5h |
| `TSK-BE-015` | Favorites / Bookmark REST Endpoints | P1 | Backend | `TSK-BE-008`, `TSK-BE-009` | 1h |
| `TSK-FE-015` | Place Detail Modal Component (`PlaceDetailModal.vue`) | P0 | Frontend | `TSK-FE-014` | 2h |
| `TSK-FE-016` | Favorites / Bookmark Pinia Store & Heart Button | P1 | Frontend | `TSK-FE-008` | 1h |
| `TSK-FE-017` | Place Review Form & List Component (`ReviewSection.vue`) | P1 | Frontend | `TSK-FE-015` | 1.5h |
| `TSK-FE-018` | Mobile Bottom Navigation Bar & Drawer Sheet (`AppLayout.vue`) | P0 | Frontend | `TSK-FE-004`, `TSK-FE-005` | 2h |
| `TSK-FE-019` | Dark Mode Theme Switcher (`ThemeToggle.vue`) | P2 | Frontend | `TSK-FE-004` | 1h |
| `TSK-QA-001` | PostGIS Spatial Radius Query Latency Test | P0 | QA | `TSK-BE-007` | 1.5h |
| `TSK-QA-002` | Smart Pet-Matching Scenario E2E Test | P0 | QA | `TSK-FE-014`, `TSK-BE-012` | 1.5h |

### Sprint 3 Definition of Done (DoD)
- [ ] 리뷰 작성 및 북마크 저장/해제 API 및 화면 정상 연동.
- [ ] 장소 상세 모달 내 공공데이터 수칙 체크리스트 및 카카오맵 길찾기 링크 정상 작동.
- [ ] 900px 이하 모바일 Viewport 전환 시 하단 네비게이션 탭 바 및 모바일 뷰 정상 연동.
- [ ] PostGIS 반경 거리 검색 Spatial Query 실행 속도 **P95 < 50ms** 검증 통과.
- [ ] 소형견(초코 4.2kg) vs 대형견(빅터 28.5kg) 대표 펫 전환 E2E 테스트 100% Pass.
