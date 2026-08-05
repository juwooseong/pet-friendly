# PetSpot AI Agents Specification (.ai/AGENTS.md)

이 문서는 **PetSpot (공공데이터 기반 펫 프렌들리 서비스)** 프로젝트를 수행하는 AI Agent 팀의 역할, 책임, 입력/출력물, 금지 사항 및 완료 조건(Definition of Done)을 규정하는 마스터 에이전트 지침서입니다.

---

## 🛠️ 공통 기술 스택 및 프로젝트 컨텍스트
- **Frontend**: Vue 3 (Composition API `<script setup lang="ts">`), TypeScript 5.x (Strict), Vite, Pinia, Vue Router 4, Leaflet.js
- **Backend**: Spring Boot 3.2+ (Java 17/21), Spring Data JPA, QueryDSL, Spring Security + JWT, Spring Batch (`@Scheduled`)
- **Database & Cache**: PostgreSQL 15+ (PostGIS `GEOMETRY(Point, 4326)` Spatial Index), Redis (Spring Cache)
- **컨텍스트 문서 참조 경로**: `SYSTEM_DESIGN.md`, `docs/` (`PRODUCT.md`, `DOMAIN.md`, `TECH_STACK.md`, `RULES.md`, `TASKS.md`, `DECISIONS.md`, `ROADMAP.md`)

---

## 1. Staff System Architect Agent (`architect-agent`)

### 1.1 책임 (Responsibilities)
- 전체 제품 아키텍처, 데이터베이스 스키마 및 REST API 계약(Swagger/OpenAPI Spec) 수립.
- 스마트 펫 매칭 알고리즘(`PASS`/`WARN`/`DENY`)의 도메인 로직 검증 및 NFR 성능 기준 수립.
- 기술 스택 변경 시 ADR(`docs/DECISIONS.md`) 작성 및 세부 문서(`docs/`) 간 컨텍스트 일관성 유지.

### 1.2 입력 문서 (Input Documents)
- `SYSTEM_DESIGN.md`, `docs/PRODUCT.md`, `docs/DOMAIN.md`, `docs/TECH_STACK.md`, `docs/DECISIONS.md`

### 1.3 출력물 (Outputs / Deliverables)
- REST API OpenAPI/Swagger 명세서
- PostgreSQL / PostGIS DDL 스키마 및 마이그레이션 Script
- `docs/DECISIONS.md` 내 신규 ADR 기록

### 1.4 금지 사항 (Prohibitions)
- 사전 데이터 모델 검증이나 사용자 승인 없이 DB 엔티티 파괴적 수정(Dropping Table/Column) 금지.
- NFR 목표(위치 반경 검색 P95 < 50ms)에 위배되는 Spatial Index 미적용 쿼리 설계 금지.

### 1.5 완료 조건 (Definition of Done)
- [ ] OpenAPI 스펙과 `docs/DOMAIN.md` 내 TypeScript Interface 및 Java Entity 필드가 100% 매핑됨.
- [ ] 모든 아키텍처 변경점이 `docs/DECISIONS.md` ADR로 문서화됨.

---

## 2. Backend Developer Agent (`backend-agent`)

### 2.1 책임 (Responsibilities)
- Spring Boot 3 기반의 REST API, Spring Security + JWT 인증 및 데이터 처리 레이어 구축.
- QueryDSL 및 Hibernate Spatial을 활용한 PostGIS 위도/경도 반경 거리 검색(`ST_DWithin`) 쿼리 구현.
- 공공데이터포털 Open API 수집 및 DB Upsert 파이프라인 배치(`@Scheduled` Batch Ingestion) 개발.
- 3단계 스마트 펫 매칭 알고리즘(`evaluatePetMatch`) 구현.

### 2.2 입력 문서 (Input Documents)
- `docs/DOMAIN.md`, `docs/TECH_STACK.md`, `docs/RULES.md`, `SYSTEM_DESIGN.md`

### 2.3 출력물 (Outputs / Deliverables)
- Spring Boot 3 도메인 패키지 코드 (`com.petspot.domain.*`)
- JPA Entity, Repository, QueryDSL Custom Impl, DTO, Service, Controller
- Spring Batch / Scheduled Ingestion Service (`com.petspot.infrastructure.publicdata`)
- JUnit 5 / Mockito 단위 테스트 및 SpringBootTest integration test 코드

### 2.4 금지 사항 (Prohibitions)
- Controller 레이어에서 Entity 직접 노출 금지 (반드시 Request/Response DTO로 변환).
- JPA Entity 내 `@Setter` 어노테이션 사용 금지 (도메인 메서드 사용).
- 위치 조회 쿼리 시 PostGIS Spatial Index 없이 전체 테이블 Full Scan(`Seq Scan`) 금지.

### 2.5 완료 조건 (Definition of Done)
- [ ] `./gradlew test` 실행 시 모든 단위/통합 테스트 100% 통과.
- [ ] REST API 응답 규격이 `docs/RULES.md` 표준 (`{ "success": true, "data": ... }`)을 준수함.
- [ ] 위치 반경 검색 API의 쿼리 실행 시간이 **P95 < 50ms** 달성.

---

## 3. Frontend Developer Agent (`frontend-agent`)

### 3.1 책임 (Responsibilities)
- Vue 3 + TypeScript 기반 SPA 및 반응형 웹/모바일 UI 컴포넌트 개발.
- Pinia 전역 스토어 (`useAuthStore`, `usePetStore`, `usePlaceStore`) 구축 및 상태 관리.
- Leaflet.js / 카카오맵 커스텀 마커 시각화 및 스마트 펫 매칭 뱃지(`PASS`/`WARN`/`DENY`) 실시간 리렌더링.
- 듀얼 반응형 브레이크포인트 (`900px`) 기준 데스크톱 Split View & 모바일 Bottom Navigation/Sheet Drawer 구현.

### 3.2 입력 문서 (Input Documents)
- `docs/DOMAIN.md`, `docs/TECH_STACK.md`, `docs/RULES.md`, `css/variables.css`

### 3.3 출력물 (Outputs / Deliverables)
- Vue 3 SFC 컴포넌트 (`<script setup lang="ts">`)
- TypeScript 인터페이스 파일 (`src/types/*.ts`)
- Pinia Stores (`src/stores/*.ts`)
- Scoped CSS 및 반응형 디자인 시스템 스타일

### 3.4 금지 사항 (Prohibitions)
- TypeScript Strict Mode 위반 및 `any` 타입 사용 금지 (반드시 Strict Interface/Type 정의).
- 전역 CSS 변수(`css/variables.css`)를 무시하고 컴포넌트 내 임의의 헥사 코드 Hardcoding 금지.
- 대표 펫(`activePet`) 변경 시 전역 상태 전파 없이 로컬 컴포넌트 내부 상태만 고립 변경 금지.

### 3.5 완료 조건 (Definition of Done)
- [ ] `npm run type-check` (`vue-tsc`) 및 `npm run build` 오류 없이 clean pass.
- [ ] 대표 펫 전환 시 카드 및 모달 내 매칭 뱃지가 즉시 리렌더링됨.
- [ ] 1440px 데스크톱 및 375px 모바일 Viewport 모두에서 레이아웃 붕괴 없음.

---

## 4. QA & Test Automation Agent (`qa-agent`)

### 4.1 책임 (Responsibilities)
- 스마트 펫 매칭 알고리즘 시나리오별 엣지 케이스 테스트 (소형/중형/대형견, 체중 초과, 접종 미완료).
- PostGIS Spatial Query 반경 검색 성능 측정 및 SLA(99.99%) 가용성 테스트.
- 백엔드 REST API - 프론트엔드 Vue 3 앱 간 E2E 통신 및 모바일 브라우저 호환성 검증.

### 4.2 입력 문서 (Input Documents)
- `docs/PRODUCT.md`, `docs/DOMAIN.md`, `docs/TASKS.md`, `docs/ROADMAP.md`

### 4.3 출력물 (Outputs / Deliverables)
- E2E 및 API 테스트 시나리오 보고서
- 위치 반경 검색 Latency 성능 측정 결과 (P95, P99 Latency Log)
- `docs/TASKS.md` 내 검증 항목 업데이트

### 4.4 금지 사항 (Prohibitions)
- 명확한 실패 로그나 렌더링 증적 없이 테스트 통과(False Positive) 선언 금지.
- 장애 상황(외부 공공데이터 API Down)에서 Local Fallback 검증 생략 금지.

### 4.5 완료 조건 (Definition of Done)
- [ ] `docs/TASKS.md` 검증 체크리스트 항목 100% 통과 완료.
- [ ] 소형견/대형견 대표 펫 전환 시 매칭 뱃지 실시간 연동 100% 검증.
