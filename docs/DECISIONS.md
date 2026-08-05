# Architecture Decision Records (DECISIONS.md)

## ADR-001: 대표 펫(Active Pet) 기반 스마트 매칭 엔진 채택
- **Status**: Accepted
- **Context**: 반려인이 여러 마리의 펫(예: 4kg 소형견 초코 & 28kg 대형견 빅터)을 소유하고 있는 경우, 모든 장소에서 각 펫에 따른 동반 제한(체중, 준비물)이 서로 달라서 매번 검색 조건을 바꾸기 불편한 문제.
- **Decision**:
  - 회원 프로필에 다중 펫 등록을 지원하되, 상단 헤더에 **'대표 펫(Active Pet)'** 칩을 고정.
  - 대표 펫 전환 시 이벤트를 전파하여 장소 카드 및 상세 화면의 동반 가능 적합도(`PASS`/`WARN`/`DENY`)가 즉시 재계산되도록 설계.
- **Consequences**: 사용자 경험이 대폭 향상되고, 매장 방문 후 동반 거절당하는 사고 방지.

---

## ADR-002: 공공데이터 수집 및 캐싱 파이프라인 (Batch Ingestion vs Real-time)
- **Status**: Accepted
- **Context**: 공공데이터포털 및 한국관광공사 Open API는 외부 응답 속도가 불확실하고 장애 발생 가능성이 존재함.
- **Decision**:
  - 공공데이터포털 API를 실시간 호출하지 않고, **배치 인제스천(Batch Ingestion)** 방식으로 주기적 파싱 후 자사 PostgreSQL + PostGIS 데이터베이스에 저장.
  - 외부 API 장애 발생 시에도 서비스 가동률 **99.99%** 보장.
- **Consequences**: 조회 속도 P95 < 50ms 달성 및 안정적 운영 가능.

---

## ADR-003: 듀얼 레이아웃 기반 반응형 웹/모바일 아키텍처
- **Status**: Accepted
- **Context**: 데스크톱 사용자는 지도와 리스트를 한눈에 보는 고해상도 스플릿 뷰를 선호하고, 모바일 사용자는 한 손 터치 및 하단 탭 바를 선호함.
- **Decision**:
  - Single Page Application 아키텍처 내에서 CSS Breakpoint (`900px`) 기준 데스크톱 Split View와 모바일 Bottom Navigation Bar + Sheet Drawer 뷰를 자동 전환하도록 설계.
- **Consequences**: 모바일 웹 및 PWA 앱 환경 지원에 최적화됨.

---

## ADR-004: 프론트엔드 Vue 3 & 백엔드 Spring Boot 3 기술 스택 확정
- **Status**: Accepted
- **Context**: 유지보수성, 대규모 트랜잭션 안전성, 공간 데이터 인덱싱(PostGIS/Hibernate Spatial)의 유연한 처리 및 프론트엔드의 반응형 상태 관리(Pinia) 필요성.
- **Decision**:
  - **Frontend**: Vue 3 (Composition API, Vite, Pinia, Vue Router)
  - **Backend**: Spring Boot 3.x (Java 17/21, Spring Data JPA, QueryDSL, Spring Security + JWT, Hibernate Spatial)
- **Consequences**:
  - Vue 3의 반응성 시스템(Reactivity)으로 펫 변경 시 UI 매칭 뱃지 리렌더링 속도 향상.
  - Spring Boot 3와 PostGIS 통합으로 위도/경도 위치 기반 반경 검색 미세 튜닝 가능.

---

## ADR-005: 프론트엔드 TypeScript 도입을 통한 데이터 일관성 및 정적 타입 검증
- **Status**: Accepted
- **Context**: 백엔드 REST API 응답 모델(`User`, `Pet`, `Place`, `PetPolicy`, `MatchResult`)과 프론트엔드 컴포넌트/스토어 상태 간의 데이터 스키마 불일치로 인한 런타임 Null Dereference 또는 타입 오류 방지 필요.
- **Decision**:
  - 프론트엔드에 **TypeScript 5.x** 도입 (`<script setup lang="ts">`, `@/types/` 스키마 정의, `vue-tsc` 정적 검사).
  - No `any` strict typing 정책을 적용하여 클라이언트와 서버 간 데이터 일관성 유지.
- **Consequences**: 컴파일 타임 오류 사전 감지, IDE 자동완성 지원 및 유지보수 생산성 대폭 향상.
