# Context Summary Generation Guide & Criteria (`CONTEXT_SUMMARY_GUIDE.md`)

이 문서는 프로젝트가 진행됨에 따라 축적되는 도메인 모델 및 기술 스택 정보를 토큰 효율적으로 인덱싱하고 요약본(`DOMAIN_SUMMARY.md`, `TECH_STACK_SUMMARY.md`)을 자동/수동 갱신하기 위한 생성 기준 가이드라인입니다.

---

## 1. Context Summary 생성 및 갱신 기준 (Criteria & Triggers)

### 1.1 `DOMAIN_SUMMARY.md` (도메인 엑기스 요약본)
- **생성/갱신 트리거 조건**:
  1. Flyway DB Migration 스크립트(`V1__...`) 변경으로 테이블/컬럼 추가 또는 스키마 변형이 발생한 경우.
  2. `@/types/` 하위 TypeScript Interface (`User`, `Pet`, `Place`, `PetPolicy`, `MatchResult`)가 추가/수정된 경우.
  3. 스마트 펫 매칭 알고리즘(`PASS`/`WARN`/`DENY`)의 조건문(체중 한계, 예방접종 필수 등)이나 견종 크기(소/중/대형) 산출 로직이 변경된 경우.
- **포함 필수 요소**: DB 엔티티 1줄 요약표, TS 핵심 인터페이스 정의, 매칭 평가 3단계 요약 규칙.

### 1.2 `TECH_STACK_SUMMARY.md` (기술 스택 엑기스 요약본)
- **생성/갱신 트리거 조건**:
  1. Spring Boot 3 또는 Vue 3 의존성(Gradle `build.gradle`, `package.json`) 및 메이저 라이브러리가 추가/버전 변경된 경우.
  2. PostGIS Spatial Query (`ST_DWithin`) 인덱싱 방식이나 Hibernate Spatial 매핑 방식이 변경된 경우.
  3. Redis Caching (`@Cacheable`) 정책 또는 NFR 성능 목표(Query Latency P95 < 50ms)가 업데이트된 경우.
- **포함 필수 요소**: 핵심 기술 버전표, 백엔드/프론트엔드 패키지 레이아웃 요약, NFR 성능 수치.

---

## 2. Summary 파일 위치 및 관리
- `docs/summaries/DOMAIN_SUMMARY.md`
- `docs/summaries/TECH_STACK_SUMMARY.md`
