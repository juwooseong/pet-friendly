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

---

## ADR-006: 회원가입 실시간 이메일/닉네임 중복확인 API의 계정 존재 여부 노출 트레이드오프 수용 (TSK-BE-028/TSK-FE-013)
- **Status**: Accepted
- **Context**: 회원가입 UX 개선을 위해 `GET /api/v1/auth/check-email`, `GET /api/v1/auth/check-nickname` 실시간 중복확인 API를 추가했다. 이 API는 "해당 이메일로 이미 가입된 계정이 있는가"를 응답으로 직접 알려주므로, `find-password`(비밀번호 찾기)에 적용한 "계정 존재 여부 비노출" 원칙과 정면으로 상충한다.
- **Decision**:
  - 실시간 중복확인은 대부분의 서비스가 채택하는 표준 UX이며, 이메일 주소는 애초에 회원가입 폼 자체에서 값을 알아야 입력할 수 있는 정보이므로(무작위 대량 조회로 계정 존재 여부를 스캔하는 것과는 위험도가 다름), 이 트레이드오프를 의도적으로 수용한다.
  - Rate Limiting(요청 빈도 제한)은 이번 Task 범위에서 구현하지 않고, Sprint 4 `TSK-INF-003`(Redis Caching for Geo-search & JWT Tokens) 또는 별도 보안 강화 Task의 후속 작업으로 미룬다.
  - 프런트엔드는 실시간 체크의 `available: true` 결과를 신뢰하여 최종 중복 검사를 생략하지 않는다 — `POST /auth/register`의 서버 측 최종 중복 검사(이메일/닉네임)를 항상 그대로 유지해, 실시간 체크 이후 경합 상태(Race Condition)로 다른 사용자가 먼저 가입한 경우에도 서버가 최종적으로 방어하도록 한다.
- **Consequences**: 무제한 요청 시 계정 존재 여부를 이메일 단위로 열거(enumeration)당할 수 있는 잔여 위험이 있으며, 이는 Rate Limiting 도입 전까지 인지된 채로 남는다.

---

## ADR-007: TourAPI vs PetSpot 데이터 소유권 및 반려동물 정책 보강 전략 (`TSK-DATA-006`)
- **Status**: Accepted — **일부 대체됨 (ADR-009, 2026-11-19)**. "TourAPI는 반려동물 정책 필드를 제공하지 않는다"는 Context의 전제는 틀렸다(`detailPetTour2` 미검증). NULL 원칙(미확인=null), 필드별 소유권 구분, 사용자 제보/관리자 보강 구조는 그대로 유효하며, 정책 필드의 1차 출처만 ADR-009로 바뀐다.
- **Context**: `TSK-DATA-005`에서 한국관광공사 `KorPetTourService2`(`areaBasedList2`/`detailIntro2`/`detailInfo2`/`detailCommon2`)를 실제 호출로 검증한 결과, TourAPI는 장소 기본정보(`contentid`/`title`/`addr1`/`addr2`/`tel`/`mapx`/`mapy`/`firstimage`/`modifiedtime`/`createdtime`/category 계열)는 충분히 제공하지만, `Place`의 반려동물 정책 필드(`maxWeightLimitKg`/`allowedSizes`/`isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone`)는 직접 제공하지 않는다. 기존 코드가 가정했던 `acmpyTypeCd` 등 7개 필드도 실제로 존재하지 않음이 확인되었다. 그럼에도 현재 `TourApiPlaceMapper.toEntity()`는 `isIndoorAllowed=true`/`isOutdoorAllowed=true`/`hasOffLeashZone=false`를 **하드코딩된 상수로 채우고 있고**, `Place`의 `@Builder` 생성자는 이 세 필드와 `allowedSizes`에 `null`이 들어오면 각각 `true`/`true`/`false`/`["SMALL","MEDIUM","LARGE"]`(전체 허용)로 **자동 치환**한다(`maxWeightLimitKg`만 유일하게 `null`을 그대로 유지). 즉 현재 코드는 "정보 없음"과 "실제로 전부 허용됨"을 구조적으로 구분하지 못하며, 이 상태로 대량 적재를 하면 근거 없는 허용 정보가 사실처럼 노출되는 위험이 있다. 프런트엔드 설계 문서(`docs/DOMAIN.md`의 `PetPolicy`/`evaluatePetMatch`)도 현재 `allowedSizes`/`isIndoorAllowed` 등을 non-nullable 필수 값으로 정의하고 있어 동일한 갭을 안고 있다.
- **Decision**:

  ### 1. Source of Truth 정의 (필드별)
  | 필드 | Source of Truth | 근거 |
  |---|---|---|
  | `publicDataId`/`name`/`category`/`categoryName`/`address`/`location`/`latitude`/`longitude`/`phone`/`imageUrl`/`operatingHours` | **TourAPI 직접 제공** | `areaBasedList2`/`detailCommon2` 실제 응답에서 확인됨(`TSK-DATA-005`) |
  | `description` | **TourAPI 가공 → 향후 PetSpot 자체 관리로 전환 검토** | 현재 `TourApiPlaceMapper`가 존재하지 않는 `claAcmpyInfo`(`petPolicyInfo`)를 `description`에 매핑하고 있어(`TourApiPlaceMapper.java:60`) 사실상 항상 `null`이 채워진다 — 이 매핑 자체가 `TSK-DATA-005` 결과와 모순되므로 **결정 필요**(실제 구현 수정은 별도 후속 작업, 이번 ADR은 문제만 기록) |
  | `maxWeightLimitKg` | **아직 데이터 없음** (TourAPI로 채울 수 없음, `TSK-DATA-005` 판정 C/D) | 구조적으로는 이미 `NULL` 유지가 가능(아래 2번 참조) — 실제 값 확보는 PetSpot 보강 필요 |
  | `allowedSizes` | **아직 데이터 없음** → 확보 전까지 **PetSpot 자체 관리**(운영/보강 프로세스로 채움) | 동일 |
  | `isIndoorAllowed` / `isOutdoorAllowed` / `hasOffLeashZone` | **아직 데이터 없음** → 확보 전까지 **PetSpot 자체 관리** | 동일. 현재 `TourApiPlaceMapper`가 `true`/`true`/`false`로 하드코딩하는 것은 **근거 없는 가정 데이터를 사실처럼 저장하는 버그**로 분류하며, 실제 값 없이는 이 필드들을 채우지 않는 방향(=null 유지)으로 전환해야 한다 |
  | 반려동물 정책 관련 전체(체중/크기/실내외/오프리쉬) | 향후 **사용자/운영자 검증 필요** 단계를 거쳐 PetSpot 자체 관리로 승격 | 아래 4번 보강 전략 참조 |

  ### 2. NULL vs FALSE 구분 원칙
  - **원칙**: `null` = 아직 확인되지 않음(미검증), `false` = 실제 근거로 불가능함이 확인됨, `true` = 실제 근거로 가능함이 확인됨. 이 원칙은 `maxWeightLimitKg`(현재도 `null` 유지 가능)와 DB 컬럼 자체(`is_indoor_allowed`/`is_outdoor_allowed`/`has_off_leash_zone`/`allowed_sizes` 모두 `NOT NULL` 제약이 없음, `V1__init_schema.sql`)에는 **이미 구조적으로 적용 가능**하다.
  - **현재 구조의 실제 문제**: `Place`의 `@Builder` 생성자(`Place.java:126-129`)가 `allowedSizes`/`isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone`에 대해 `null` 입력을 애플리케이션 레벨에서 자동으로 "전체 허용값"으로 치환하고 있어, DB 컬럼이 nullable이어도 Java 계층에서 이미 `null`이 사라진다. 이는 NULL/FALSE 구분 원칙과 **정면으로 상충**한다.
  - **이번 작업에서 Entity를 변경하지 않는다는 범위 제한 하의 결론**: 원칙 자체는 채택하되, 이를 코드에 반영(생성자의 기본값 치환 로직 제거/조건부화, `TourApiPlaceMapper`의 하드코딩 제거)하는 것은 **후속 작업으로 이관**한다(`TSK-DATA-008`, 아래 신규 백로그).
  - `docs/DOMAIN.md`의 `PetPolicy` 프런트엔드 타입도 `allowedSizes: SizeCategory[]`/`isIndoorAllowed: boolean` 등 non-nullable로 정의되어 있어 백엔드가 `null`을 내려도 프런트가 이를 표현할 방법이 없다 — `allowedSizes: SizeCategory[] | null`, `isIndoorAllowed: boolean | null` 등으로의 타입 변경도 후속 작업 범위(**프런트엔드 변경은 이번 TSK-DATA-006에서 금지**).

  ### 3. `allowedSizes` 값 표준
  - 이미 프로젝트에 정의된 `PetSizeCategory` enum(`backend/src/main/java/com/petspot/domain/pet/entity/PetSizeCategory.java`)을 그대로 사용한다 — 새로 정의하지 않는다.
    - `SMALL`: 체중 10kg 이하
    - `MEDIUM`: 10kg 초과 20kg 이하
    - `LARGE`: 20kg 초과
  - `Place.allowedSizes`(JSONB, `List<String>`)에는 이 enum의 `name()` 문자열만 저장한다(`PlaceSearchCondition`의 `@Pattern(regexp = "SMALL|MEDIUM|LARGE")`, `docs/DOMAIN.md`의 `SizeCategory` 타입과도 이미 일치).
  - JSONB를 유지하는 이유: (a) "복수 크기 허용"이라는 배열 값의 성격상 정규화 테이블보다 조회 패턴이 단순(포함 여부 검사, `QPlace.place.allowedSizes` JSONB 연산자 기반 검색이 이미 `PlaceRepositoryImpl`에 구현됨), (b) 값의 후보군이 3개로 고정되어 있어 참조 무결성이 크게 필요하지 않음. 정규화가 필요한 시점은 "크기 카테고리 자체가 장소별로 커스터마이즈"되는 요구사항이 생길 때로 한정한다(현재는 없음).
  - `null` = 아직 검증 안 됨, `[]`(빈 배열) = 실제 근거로 "허용 크기 없음(모든 크기 불가)"이 확인된 경우로 의미를 분리한다(이 구분도 후속 작업에서 코드/문서에 반영).

  ### 4. 반려동물 정책 보강 방법 비교
  | 방법 | 신뢰도 | 구현 난이도 | 유지보수 비용 | 최신성 | 오정보 위험 |
  |---|---|---|---|---|---|
  | A. 관리자/운영자 수동 입력 | 중 (사람이 직접 판단, 근거 문서화 안 될 수 있음) | 낮음(어드민 화면/DB 직접 입력) | 높음(장소 수 증가 시 병목) | 낮음(수동 갱신 주기에 의존) | 중(오해/오타 가능) |
  | B. 장소 운영자가 직접 제공 | 이론상 최고(당사자 정보) | 높음(운영자 인증/제출 플로우 필요) | 중(운영자가 스스로 갱신) | 중~높음(운영자 동기 부여 필요) | 중(운영자가 실제보다 관대하게 표기할 유인 존재) |
  | C. 사용자 제보/검증 | 낮음(개별 제보는 신뢰도 낮음)~중(다수 제보 합의 시) | 중(제보 UI + 집계 로직) | 중(악의적/오래된 제보 필터링 필요) | 높음(실시간성) | 높음(단일 제보 오신고 위험, 검증 체계 필요) |
  | D. 공식 홈페이지/전화 등 외부 근거 기반 운영자(PetSpot 내부) 검증 | 높음(근거 URL/통화 기록 남김) | 중(검증 프로세스+근거 저장 필드 필요) | 높음(사람이 직접 조사) | 낮음(조사 인력만큼만 확장) | 낮음(근거가 남아 추적 가능) |
  | E. 향후 AI 추출/보조(리뷰·홈페이지 텍스트에서 정책 추출) | 낮음~중(현 시점 미검증, 환각 위험) | 높음(모델/파이프라인 구축) | 낮음(자동화 이후에는 저비용) | 높음(자동 재수집 가능) | 높음(사람 검증 없이는 오정보 유입 가능, `WARN` 이하로만 취급 필요) |
  - **권장 조합**: 초기(현재 단계)에는 **D(외부 근거 기반 내부 검증) 우선 + A(관리자 입력 도구)**로 신뢰도 높은 데이터부터 소량 확보하고, **C(사용자 제보)** 는 "검증 대기" 상태로만 반영(자동으로 정책값을 덮어쓰지 않음). **E(AI)** 는 `TSK-AI-*`(Sprint 4, ADR 범위 밖) 이후 "제안값 생성"용으로만 검토하며, 사람 검증 없이 `true`/`false` 확정값으로 승격시키지 않는다.

  ### 5. PASS / WARN / DENY와 데이터 부족의 관계 (판정 원칙, 코드 미구현)
  - 기존 프런트엔드 설계(`docs/DOMAIN.md`의 `evaluatePetMatch`)는 이미 `PASS`/`WARN`/`DENY`/`INFO` 4가지 상태를 정의하고 있으나, 현재 로직은 `maxWeightLimitKg`/`allowedSizes`가 항상 값이 있다고 가정한다(`null` 데이터 부족 케이스를 다루지 않음). 이 ADR은 데이터 부족 시의 판정 원칙만 아래와 같이 정의한다(실제 로직 수정은 코드 구현 금지 범위이므로 후속 작업):
    - **DENY (명확한 제한)**: 근거 있는 값이 존재하고, 그 값이 반려동물 조건에 위반될 때만. 예: `maxWeightLimitKg`가 실제 값(`null` 아님)이고 펫 체중 초과, 또는 `allowedSizes`가 실제 값(`null`도 `[]`도 아님)이고 펫 크기가 미포함.
    - **PASS (명확한 허용)**: 관련된 모든 정책 필드가 `null`이 아니며, 펫이 모든 조건을 만족할 때만. `null` 필드가 하나라도 있으면 `PASS`로 판정하지 않는다(근거 없는 허용 노출 방지).
    - **WARN (정보 부족/조건부)**: 관련 정책 필드 중 하나 이상이 `null`(미검증)이고, 확인된 필드에서는 위반이 없을 때. 기존 `INFO`(펫 미등록)와는 별개로, "펫은 있는데 장소 정책이 미검증"인 경우를 `WARN`으로 명확히 한다 — 예: `⚠️ 이 장소의 반려동물 정책이 아직 확인되지 않았습니다. 방문 전 문의를 권장합니다.`
    - **정리**: `null` = 항상 `WARN` 쪽으로 수렴(보수적 처리), `DENY`/`PASS`는 반드시 근거(`false`가 아닌 실제 검증된 값)가 있어야만 선언한다. "정보 없음을 `PASS`로 처리"하는 것은 금지 원칙으로 명문화한다(실제 방문 거절 사고 방지가 제품 목표이므로).

  ### 6. TourAPI 동기화와 PetSpot 정책 데이터의 소유권 분리
  ```
  TourAPI sync (주기 배치, TSK-BE-005/ADR-002)
      ↓
  장소 기본정보 컬럼만 갱신
  (name/category/categoryName/address/location/latitude/longitude/phone/imageUrl 등)
      ↓
  반려동물 정책 컬럼(maxWeightLimitKg/allowedSizes/isIndoorAllowed/isOutdoorAllowed/
  hasOffLeashZone)은 TourAPI sync 경로에서 절대 쓰지 않음(UPSERT 시 해당 컬럼 제외)
      ↓
  PetSpot 정책 데이터는 별도 경로(관리자 도구/운영자 제출/검증 완료 제보)로만 갱신
      ↓
  TourAPI 재동기화가 반복되어도 정책 컬럼은 보존됨
  ```
  - 현재 `TourDataBatch`/`TourDataCollectorService`/`TourApiPlaceMapper`는 아직 UPSERT(갱신) 로직이 없어(최초 적재만 대상) 이 문제가 실제로 발생하지 않았으나, `TSK-DATA-003`(초기 적재) 이후 **재동기화 배치를 구현하는 시점**에 반드시 "정책 컬럼은 갱신 대상에서 제외"하는 조건을 넣어야 한다. 이는 **Update 쿼리/매퍼 설계 단계에서부터 구조적으로 강제**해야 한다(예: `updateFromTourApi(dto)` 같은 전용 도메인 메서드를 만들어 기본정보 컬럼만 파라미터로 받고, 정책 컬럼은 시그니처에 아예 존재하지 않게 한다 — 현재 `Place.updateDetails()`가 이미 이 패턴을 따르고 있음(`name`/`address`/`location`/`imageUrl`/`phone`/`categoryName`만 갱신, 정책 필드는 건드리지 않음) → **후속 재동기화 로직도 이 기존 패턴을 그대로 확장**하면 된다는 것이 이번 설계의 핵심 결론).

  ### 7. 데이터 변경 추적을 위한 메타데이터 설계 (Entity 구현은 후속 작업)
  - 다음 4가지 상황을 구분하려면 최소한 아래 메타데이터가 필요하다(현재는 전혀 없음 — `updatedAt` 하나로 모든 변경 사유가 뭉뚱그려짐):
    - TourAPI가 장소 기본정보를 변경 → `basicInfoSyncedAt`(또는 기존 `updatedAt`을 기본정보 전용으로 재정의)
    - 운영자가 정책을 변경 → `policyVerifiedAt` + `policyVerifiedBy`(운영자 식별자) + `policySource`(예: `OPERATOR_PHONE`/`OFFICIAL_WEBSITE`/`ADMIN_MANUAL`)
    - 사용자가 정책을 제보 → 정책 컬럼에 즉시 반영하지 않고 별도 "제보" 레코드(향후 `PlacePolicySuggestion` 성격의 구조)로 분리 보관, 운영자 승인 후에만 `policyVerifiedAt` 갱신과 함께 반영
    - 정책 정보가 오래되어 재검증 필요 → `policyVerifiedAt` 기준 만료 정책(예: 6개월 경과 시 "재검증 필요" 배지)을 운영 정책으로 정의(구체적 기간은 **결정 필요** — 제품팀 확인 필요)
  - 이 메타데이터를 `Place`에 직접 컬럼으로 추가할지, 별도 `PlacePolicy`/`PlacePolicyHistory` 엔티티로 분리할지는 아래 8번 평가에서 다룬다.

  ### 8. 현재 `Place` Entity 평가
  - **지금 구조로 충분한 부분**: 기본정보(`name`/`category`/`address`/`location`/좌표/`phone`/`imageUrl`), 리뷰 집계(`rating`/`reviewCount`, `updateRatingAndReviewCount()`로 이미 책임 분리됨), `allowedSizes`(3개 고정 후보값의 JSONB 배열)는 현재 구조 유지로 충분.
  - **향후 별도 `PetPolicy` 엔티티가 필요한 부분**: 정책 데이터의 **출처/근거/검증자/검증 시각/재검증 주기**(7번의 메타데이터)는 `Place`에 컬럼을 계속 추가하는 방식보다 `PlacePolicy`(1:1 또는 1:N 이력 테이블)로 분리하는 것이 적합하다 — 이유: (a) `Place`는 "장소"라는 서로 다른 책임(TourAPI 동기화 대상)을 가지므로 정책 검증 이력이 섞이면 엔티티가 두 가지 변경 이유(TourAPI 동기화 vs 정책 검증)를 갖게 되어 SRP 위반, (b) 향후 "정책 변경 이력 조회"(누가 언제 무슨 근거로 바꿨는지)가 제품 요구사항이 될 가능성이 높은데 이는 이력 테이블 없이는 불가능.
  - **JSONB로 유지해도 되는 부분**: `allowedSizes`(후보값 고정, 배열), `policyChecklist`(스키마 마이그레이션 V1에 이미 존재하는 컬럼이나 현재 Entity/매퍼에는 아직 매핑되지 않음 — 이것도 **결정 필요**: 이 컬럼을 이번 정책 보강 전략에 포함시킬지, 별도 작업으로 남길지).
  - **정규화가 필요한 부분**: 정책의 근거(출처 URL/통화 메모/검증자)와 이력은 위 7번처럼 별도 테이블이 적합. 단일 컬럼(JSONB나 TEXT)에 우겨넣으면 "가장 최근 검증이 언제였는지" 같은 질의가 비효율적이고, 감사(audit) 요구사항 충족이 어렵다.
  - **결론**: 이번 작업에서는 Entity를 변경하지 않되, 다음 설계를 후속 작업(`TSK-DATA-008`)의 출발점으로 남긴다 — `PlacePolicy`(또는 유사 이름) 신규 엔티티: `placeId`(FK), 정책 값 5종(현재 `Place`에 있는 것과 동일 컬럼), `source`(enum), `verifiedBy`, `verifiedAt`, `note`(근거 텍스트/URL). `Place`는 정책 값 컬럼을 유지하되 "현재 유효한 최신 정책"의 캐시/비정규화 사본으로 재정의(조회 성능 유지, `PlaceRepositoryImpl`의 기존 JSONB 검색 쿼리 재사용 가능)하고, `PlacePolicy`가 Source of Truth 겸 이력 저장소 역할을 한다.

  ### 9. `TSK-DATA-003`(초기 적재)과의 관계 — 적재 전략 선택
  - **비교**:
    - **전략 1 — TourAPI 기본정보만 먼저 전부 적재**: 장소 수를 빠르게 확보(지도/검색 화면의 "비어 보이지 않는" MVP 달성)할 수 있으나, 정책 필드가 전부 `null`(또는 현재 코드대로면 근거 없는 `true`/`true`/`false`)인 채로 노출되어 "펫 매칭 서비스"라는 제품 핵심 가치(`docs/PRODUCT.md`의 `PASS`/`WARN`/`DENY` 자동 계산)를 초기부터 훼손할 위험이 있다. 5번 원칙(`null`은 `WARN`)을 그대로 적용하면 초기 적재 장소 대부분이 `WARN`으로만 표시되어 사용자 신뢰도가 떨어질 수 있다.
    - **전략 2 — 반려동물 정책이 확인된 장소만 적재**: 표시되는 모든 장소가 신뢰할 수 있는 `PASS`/`DENY` 판정을 받을 수 있으나, `TSK-DATA-005` 결과상 TourAPI만으로는 정책 확인이 사실상 불가능해(보강 방법 4번 참조, 특히 D는 사람이 직접 조사해야 함) 초기 장소 수가 극히 적어질 위험이 크다 — MVP 단계에서 "지도에 장소가 거의 없음" 문제가 더 치명적일 수 있다.
  - **권장 전략(선택)**: **전략 1을 기반으로 하되, 정책 필드는 반드시 `null`(미검증)로 적재하고 5번의 `WARN` 원칙을 그대로 노출한다.** 근거: (a) PetSpot 제품 목표(`docs/PRODUCT.md`)는 "매칭 적합도 표시"이지 "정책 미확인 장소를 숨기는 것"이 아니며, `WARN`도 "정보 제공"이라는 유효한 제품 가치가 있다(방문 전 문의 유도). (b) 장소 수가 0에 가까운 상태로 서비스를 시작하는 것이 사용자 신뢰도에 더 치명적이다. (c) 이후 4번의 보강 전략(특히 D)으로 인기/방문 빈도가 높은 장소부터 우선 검증해 `WARN` → `PASS`/`DENY`로 점진 전환하는 로드맵이 가능하다. **단, 전제조건**: 현재처럼 `isIndoorAllowed=true` 등을 하드코딩/자동 치환하는 코드(`TourApiPlaceMapper`, `Place` 생성자)를 먼저 고쳐 실제로 `null`이 적재되게 만들어야 한다 — 이 전제가 충족되지 않은 채로 `TSK-DATA-003`을 진행하면 안 된다(`TSK-DATA-008` 선행 필요).

  ### 10. 요약 (필수 문서화 항목)
  - TourAPI = 장소 기본정보(`name`/`address`/좌표/`phone`/`imageUrl`/category 등)의 Source of Truth.
  - PetSpot(운영자 검증/관리자 입력, 향후 사용자 제보+승인) = 반려동물 정책 데이터(`maxWeightLimitKg`/`allowedSizes`/`isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone`)의 Source of Truth.
  - `null`=미검증, `false`=근거 있는 불가, `true`=근거 있는 가능. 근거 없이 `true`/기본 허용값을 채우는 것은 금지.
  - TourAPI 재동기화는 기본정보 컬럼만 갱신하고 정책 컬럼은 절대 건드리지 않는다(`Place.updateDetails()` 패턴을 재동기화 로직에도 확장 적용).
  - `PASS`/`DENY`는 근거 있는 값에서만 선언, 관련 정책 필드에 `null`이 하나라도 있으면 `WARN`으로 보수적으로 처리한다.
- **Consequences**:
  - 초기 적재 장소 대부분이 `WARN`으로 표시될 수 있음(사용자 기대치 관리 필요, 프런트엔드 문구/UI 후속 작업 필요 — 이번 ADR 범위 밖).
  - `TourApiPlaceMapper`의 정책 필드 하드코딩과 `Place` 생성자의 null→기본값 치환 로직은 이 ADR의 원칙과 상충하므로, `TSK-DATA-003`(대량 적재) 착수 전에 반드시 `TSK-DATA-008`(아래 신규 백로그)로 먼저 수정해야 한다.
  - `PlacePolicy` 별도 엔티티 도입 여부·시점, `policy_checklist` 컬럼 처리, 정책 재검증 만료 기간은 **결정 필요** 상태로 남겨둔다(제품/운영 우선순위 확인 필요).

---

## ADR-008: TourAPI 좌표 결측 장소 처리 정책 — 적재 제외(전략 B) 채택 (`TSK-DATA-003-B`)
- **Status**: Accepted
- **Context**: `TSK-DATA-003-A`에서 실제 TourAPI 데이터를 DB에 저장하는 파이프라인을 검증하던 중, `mapx`/`mapy`가 빈 문자열이거나 숫자로 파싱할 수 없는 경우 `TourApiPlaceItemDto`/매퍼 레벨에서는 `null`로 안전하게 처리할 수 있었지만, `Place`의 `latitude`/`longitude`/`location` 컬럼이 `V1__init_schema.sql`에서 `NOT NULL`로 정의되어 있어 실제로는 `null`을 저장할 수 없다는 구조적 문제가 드러났다. 기존 `TourApiPlaceMapper.toEntity()`(TSK-DATA-002 때부터 존재)는 이를 회피하기 위해 좌표 결측 시 `0.0`/`0.0`(위경도 0,0 — 기니만 인근의 "Null Island")으로 조용히 대체해 저장하고 있었다. 이는 실제로는 위치를 알 수 없는 장소를 마치 유효한 좌표를 가진 장소처럼 지도/거리순 검색에 노출시킬 위험이 있어, "근거 없는 값을 채우지 않는다"는 `ADR-007`의 데이터 품질 원칙과 정확히 같은 종류의 문제였다.
- **Decision**:
  - **전략 A(좌표 `null` 허용) vs 전략 B(좌표 없는 장소는 적재 제외) 비교**:

    | 관점 | 전략 A: `latitude`/`longitude`/`location` nullable화 | 전략 B: 좌표 없으면 Place 자체를 저장하지 않음 |
    |---|---|---|
    | 제품 목적 부합 | 위치 기반 매칭이 핵심인 PetSpot에서 좌표 없는 장소가 존재할 이유가 낮음 | 지도/검색에 나타나는 모든 장소가 항상 유효한 위치를 가짐이 보장됨 — 제품 목적에 더 부합 |
    | 검색 API | `PlaceRepositoryImpl`의 `ST_DWithin`/`ST_Distance`/`sizeCategoryAllowed` 등 모든 QueryDSL 조건과 `PlaceSearchResponseDto`가 좌표 `null`을 전제하지 않고 짜여 있어, `null` 허용 시 공간 함수 동작을 전부 재검증해야 함 | 기존 검색 API/DTO를 전혀 건드리지 않아도 됨(항상 좌표가 있다는 기존 불변식이 그대로 유지됨) |
    | 지도 | 프런트엔드 지도 마커 렌더링 로직이 좌표 `null`을 처리하도록 별도 방어 코드가 필요해짐(범위 밖) | 지도에 좌표 없는 마커가 나타날 가능성이 원천 차단됨 — 추가 방어 코드 불필요 |
    | 거리순 정렬 | `ST_Distance` 계산에서 `location`이 `null`인 행을 제외하는 조건을 모든 정렬 쿼리에 추가해야 함 | 변경 불필요 |
    | 데이터 보존 | 장소 기본정보(이름/주소/전화 등)를 좌표 없이도 보존 가능 — 추후 좌표만 보강하면 즉시 활용 가능 | 좌표를 확보하기 전까지 해당 장소 데이터 자체가 PetSpot에 존재하지 않음(재수집 시 다시 확보) |
    | 향후 좌표 보강 | DB에 이미 레코드가 있으므로 좌표만 채우는 UPDATE로 간단히 보강 가능 | 좌표가 확보된 이후 재수집 배치를 다시 돌려야 함(기존 `saveCollectedPlaces()`의 `publicDataId` 기반 upsert로 자동 처리되므로 추가 구현 불필요) |
    | 구현 복잡도 | `V2` migration(컬럼 nullable화) + Entity 변경 + 검색/지도 관련 코드 전반의 null-safety 재검토 필요 — 이번 작업 범위(migration 최소화, 검색/지도 불변) 기준으로 복잡도 높음 | migration 없음, `TourApiPlaceMapper`/`TourDataCollectorService` 최소 수정만으로 완결 |
  - **채택**: **전략 B(좌표 없는 장소는 적재 제외)**. 근거: (1) TSK-DATA-005/TSK-DATA-003-A에서 실제 확인한 `areaBasedList2` 응답은 거의 모든 항목에 유효한 `mapx`/`mapy`를 포함하고 있어 좌표 결측은 예외적인 케이스이므로, 이 소수 사례를 위해 검색/지도 전반의 null-safety를 재검토하는 비용이 정당화되지 않는다. (2) PetSpot은 위치 기반 반려동물 동반 장소 매칭이 핵심 제품 가치이므로, 위치를 모르는 장소는 애초에 서비스 목적에 부합하지 않는다. (3) migration 없이 기존 검색/지도 불변식(좌표는 항상 유효함)을 그대로 유지할 수 있어 이번 작업의 범위 제한(불필요한 Entity/DB 구조 변경 금지)과 정확히 일치한다.
  - **0.0 fallback 제거**: `TourApiPlaceMapper.toEntity()`의 `dto.getLongitude() != null ? ... : 0.0` 대체 로직을 제거했다. 좌표를 파싱할 수 없으면(둘 중 하나라도 `null`) `toEntity()`는 이제 `dto == null`일 때와 동일하게 **`null`을 반환**한다 — 임의의 좌표를 만들어내지 않는다.
  - **적재 스킵**: `TourDataCollectorService.saveCollectedPlaces()`가 `contentId` 결측과 동일한 방식으로, `tourApiPlaceMapper.toEntity(dto)`가 `null`을 반환하면 해당 항목만 건너뛰고(`skippedCount`로 로그만 남김) 나머지 배치는 계속 진행하도록 수정했다. 새 upsert 시스템이나 별도 큐/재시도 로직은 만들지 않았다(범위 제한 준수).
  - **DB/검색/지도**: `Place.latitude`/`longitude`/`location`은 계속 `NOT NULL`로 유지(migration 없음). 좌표 없는 장소는 애초에 DB에 존재하지 않으므로 `PlaceRepositoryImpl`의 검색/거리 정렬/지도 조회 쿼리는 전혀 수정하지 않았고, `0,0` 근방 검색으로도 좌표 결측 장소가 나타나지 않음을 테스트로 확인했다(`TourDataInitialIngestionPipelineTest`).
  - **향후 좌표 보강 가능성**: 이번에 스킵된 장소는 DB에 어떤 흔적도 남기지 않으므로, TourAPI가 해당 장소의 좌표를 이후 보강하거나(예: 상세 API 재확인) PetSpot이 별도 경로로 좌표를 확보하면, 기존 `saveCollectedPlaces()`의 `publicDataId` 기반 upsert 로직을 재실행하는 것만으로 자동으로 반영된다 — 별도 추적 테이블 없이도 배치 재실행 자체가 재시도 메커니즘 역할을 한다. 스킵된 장소 목록을 별도로 기록/추적할지는 **결정 필요**(현재는 로그로만 남음, 운영 관점에서 스킵 건수 모니터링이 필요해지면 후속 작업에서 재검토).
- **Consequences**:
  - TourAPI가 제공하는 좌표 없는 장소는 PetSpot에 전혀 나타나지 않는다(장소 기본정보조차 보존되지 않음) — `TSK-DATA-003-C`(전국 적재) 시 이런 장소들의 비율이 얼마나 되는지 사전에 파악되지 않았으므로, 대량 적재 착수 시 스킵 건수를 모니터링할 필요가 있다.
  - 검색/지도/거리순 정렬 API는 전혀 변경하지 않았다 — 기존 "좌표는 항상 유효하다"는 불변식이 계속 유지된다.
  - 향후 좌표 결측 장소를 어떤 형태로든 보존해야 할 필요(예: "위치 미확인" 상태의 장소를 사용자에게 노출)가 제품 요구사항으로 생기면, 이 ADR을 재검토하고 전략 A(migration 포함)로 전환을 고려해야 한다.

## ADR-009: TourAPI `detailPetTour2`를 반려동물 정책의 1차 출처로 채택 (ADR-007 일부 대체, `TSK-DATA-009`)
- **Status**: Accepted (2026-11-19)
- **Context**:
  - ADR-007은 `TSK-DATA-005`의 결론("TourAPI는 체중/크기/실내외 등 반려동물 정책 필드를 제공하지 않는다")을 전제로 "정책 데이터는 PetSpot이 직접 구축한다"고 정했다.
  - 그러나 `TSK-DATA-005`는 `areaBasedList2`/`detailIntro2`/`detailInfo2`/`detailCommon2` 4개 오퍼레이션만 검증했고, 반려동물 전용 오퍼레이션 **`detailPetTour2`(반려동물 동반여행 조회)를 검증 대상에서 빠뜨렸다.** 그 이전 `TSK-DATA-002`(2026-09-09)는 서비스키 승인 전 상태에서 `detailPetTour2`가 `NO_OPENAPI_SERVICE_ERROR`(reasonCode 12)를 반환한 것을 근거로 "존재하지 않는 경로"라고 잘못 기록했다.
  - 2026-11-19 승인된 키로 실제 호출한 결과 `detailPetTour2`는 정상(resultCode `0000`)이며, 조회한 관광지/레포츠 10곳 **모두** 정책 텍스트를 반환했다: `acmpyPsblCpam`(동반 가능 동물: "전 견종 동반 가능", "10kg 이하 동반 가능", "15kg 미만 중소형견만 입장 가능(맹견 및 대형견 입장 불가)"), `acmpyTypeCd`(전구역/일부구역 동반가능), `acmpyNeedMtr`(목줄 착용, 이동장/유모차), `etcAcmpyInfo`(예방접종 필수, 실내 시설 입장 불가, 마릿수 제한 등), `relaPosesFclty` 등. 과거 `TourApiPlaceItemDto`의 "추측 필드" 이름 대부분이 실제로 이 오퍼레이션의 필드였다.
  - 제약: `contentId` 필수(목록 조회 불가, 장소당 1회 호출), 값은 사람이 쓴 자유 텍스트, 정책 미등록 장소는 정상 응답에 `items: ""`.
- **Decision**:
  1. **정책 필드(`maxWeightLimitKg`/`allowedSizes`/실내 출입/필요 사항)의 1차 출처를 TourAPI `detailPetTour2`로 한다.** 원문 텍스트는 해석 결과와 함께 보존한다(재해석·화면 안내용).
  2. 자유 텍스트 해석은 `TourApiPetPolicyParser`가 담당하며 **ADR-007의 NULL 원칙을 그대로 적용**한다 — 근거 문구가 없는 항목은 `null`(미확인). "목줄 언급 없음"을 "목줄 불필요(false)"로 해석하지 않는다. 체중 "N kg 미만"은 N으로 근사 저장하고 원문을 보존한다. 크기는 `PetSizeCategory` 경계(10kg/20kg)로 유도한다.
  3. ADR-007의 PetSpot 자체 관리(관리자 입력·사용자 제보)는 **보강·검증 역할**로 축소한다: TourAPI 정책이 없는 장소, 해석 결과가 `null`인 항목, 현장 정보와 다른 경우의 정정. 출처 우선순위와 재동기화 시 덮어쓰기 규칙은 `TSK-DATA-010`에서 확정한다(원칙: 관리자가 검증한 값은 TourAPI 재동기화로 덮어쓰지 않는다).
  4. 한국문화정보원 "전국 반려동물 동반 가능 문화시설 위치 데이터"(CSV, 입장 가능 동물 크기·실내/실외 여부 컬럼 보유)는 교차 검증·보강용 2차 출처 후보로 둔다(별도 작업).
- **Consequences**:
  - 직전까지의 판단 "핵심 가치(스마트 매칭)는 데이터 출처가 없어 막혀 있다"는 철회한다 — 공식 API 데이터로 시작할 수 있다.
  - `TourApiPlaceItemDto`의 추측 필드는 삭제가 아니라 `TourApiPetTourItemDto`(실제 필드명)로 이동했다(`TSK-DATA-007` 해결). 목록 매핑 단계(`TourApiPlaceMapper`)는 정책을 채우지 않는다.
  - 전국 적재(`TSK-DATA-003-C`)는 장소 수만큼 `detailPetTour2` 호출이 추가되므로 일일 트래픽 한도와 재시도/재개(resume) 설계가 필요하다.
  - 샘플 10건은 관광지/레포츠 위주다. 숙박·음식점 유형의 정책 채움률과 표현은 `TSK-DATA-010`에서 추가 확인한다.
  - 교훈: "필드가 없다"는 결론은 **해당 서비스의 전체 오퍼레이션 목록을 확인한 뒤**에만 내린다. 서비스키 승인 전의 게이트웨이 오류 코드는 경로 존재 여부의 근거로 쓰지 않는다.

## ADR-010: 장소 소개·운영시간·사진은 상세 화면 첫 조회 시 지연 적재 (2026-11-19)
- **Status**: Accepted
- **Context**: 장소 소개(`detailCommon2`), 운영시간·부가 정보(`detailIntro2`), 사진(`detailImage2`)은 장소마다 각각 호출해야 한다(목록 조회 불가). 전국 9,630곳을 미리 채우면 약 29,000회 호출이 필요해, 일일 트래픽 한도로 수 주가 걸리고 그동안 반려동물 정책 조회(`TSK-DATA-011`)와 한도를 나눠 써야 한다. 반면 사용자가 실제로 여는 장소는 일부다.
- **Decision**:
  - 공공데이터 장소의 상세 화면을 **처음 열 때** 세 오퍼레이션을 호출해 저장하고(`detail_synced_at` 기록), 이후에는 DB 값을 쓴다(`PlaceDetailEnrichmentService`).
  - 실패(한도 초과·장애)해도 상세 조회는 정상 응답하고, 조회 시각을 남기지 않아 다음 조회 때 재시도한다.
  - 외부 호출은 조회 트랜잭션 밖(컨트롤러)에서 하고 저장만 별도 트랜잭션(REQUIRES_NEW)으로 한다 — DB 커넥션을 붙잡고 네트워크를 기다리지 않기 위함.
  - 상세에서 가져오는 항목이 늘면 `detail_synced_at`을 초기화하는 마이그레이션으로 재조회를 유도한다(V11 선례).
- **Consequences**: 처음 여는 장소는 1~2초 늦게 열린다. 장소 탐색 목록 카드에는 소개·운영시간이 없다(필요해지면 우선순위 카테고리만 배치로 채우는 방안 검토).

## ADR-011: 장소 카테고리는 TourAPI 장소 유형(contentTypeId) 기준으로 매핑 (2026-11-19)
- **Status**: Accepted
- **Context**: 기존 매핑은 12/28→PARK, 32→HOTEL, 39→CAFE만 처리해 전국 적재 시 9,630곳 중 8,663곳이 ETC였다. 분류 이름은 `cat3`를 그대로 써서 "A04010600" 같은 코드이거나 빈 문자열(8,662곳)이었다.
- **Decision**: 장소 유형 8종을 모두 매핑한다 — 12 관광지·28 레포츠 → PARK(기존 호환), 14 → CULTURE, 15 → FESTIVAL, 25 → COURSE, 32 → HOTEL, 38 → SHOPPING, 39 → CAFE(음식점·카페, 기존 호환). 분류 이름은 유형 이름("관광지", "쇼핑" 등)을 쓴다. 원본 `content_type_id`도 저장해 이후 재분류가 가능하게 한다. 신규 적재와 재적재 모두 `TourApiPlaceMapper.applyListInfo()` 한 곳에서 반영한다.
- **Consequences**: 데이터의 90%(8,639곳)가 쇼핑으로 드러났다 — 탐색 기본 화면 처리 필요(`TSK-FE-020`). 새 분류 체계(`lclsSystm1~3`)는 코드표 조회가 필요해 이번에는 쓰지 않았다.

## ADR-012: 공공데이터 출처 표시와 외부 링크 안전 규칙 (2026-11-19)
- **Status**: Accepted
- **Context**: TourAPI 데이터·사진은 공공누리 조건으로 제공된다. 실제 이미지 유형은 제3유형(출처표시+변경금지) 8,922곳, 제1유형(출처표시) 549곳이다. 그동안 화면에 출처 표시가 없었다. 또한 TourAPI 홈페이지 값은 HTML 링크 조각으로 오고, 사진 URL도 외부 값이다.
- **Decision**:
  - 공공데이터 장소(`publicDataId` 있음)에 "한국관광공사 · 공공누리 n유형"을 표시한다(상세 사진은 사진별 유형). 장소 탐색 목록 하단에도 출처 문구를 둔다. 유형 코드는 저장한다(`image_copyright_type`, 사진별 `copyrightType`).
  - 외부 값은 **http/https만** 저장하고 링크로 건다(서버·화면 이중 확인), 링크는 `rel="noopener noreferrer"`. TourAPI 텍스트는 HTML을 제거한 평문으로 저장하고 화면은 텍스트로만 렌더링한다(`v-html` 금지).
- **Consequences**: 제3유형 "변경금지"와 화면의 이미지 자르기(`object-cover`)의 관계는 확인이 필요하다(`TSK-FE-021`).
- **보완 (2026-11-23, `TSK-FE-021`)**: 표시용 자르기가 "변경"에 해당하는지 해석이 갈릴 수 있어 보수적으로 처리한다. 변경금지 유형(제3·4유형) 이미지는 모든 화면(장소 탐색 카드, 장소 상세 사진·썸네일, 홈, 즐겨찾기)에서 **자르지 않고 원본 비율로**(`object-contain`, 남는 부분은 배경색) 표시하고, 제1·2유형과 유형 정보가 없는 이미지는 기존처럼 틀에 맞게 자른다(`imageFitClass`). 홈·즐겨찾기 응답에도 `imageCopyrightType`을 추가했다.

## ADR-013: JWT 무효화는 토큰 버전, 비밀번호 찾기는 재설정 링크 (2026-11-23, `TSK-SEC-001`, `TSK-SEC-003`)
- **Status**: Accepted
- **Context**: 비밀번호를 바꿔도 이전에 발급한 JWT가 만료(24시간)까지 유효했다. 또 비밀번호 찾기는 이메일만 받으면 즉시 비밀번호를 임시 비밀번호로 바꿔 메일로 보냈다 — 제3자가 남의 계정을 잠글 수 있고, 메일 발송이 실패하면 사용자가 스스로 복구할 수 없으며, 비밀번호가 메일에 평문으로 남았다.
- **Decision**:
  - **토큰 버전**: `users.token_version`을 토큰의 `ver` 클레임과 비교한다. 비밀번호가 바뀌면 버전을 올려 이전 토큰을 모두 무효화한다. Redis 블랙리스트 대신 고른 이유는 인증 필터가 이미 매 요청 사용자를 DB에서 읽고 있어 추가 조회·인프라가 없기 때문. 버전 없는 기존 토큰은 0으로 본다(배포 시 로그인 유지). 비밀번호를 바꾼 기기에는 새 토큰을 돌려준다.
  - **재설정 링크**: 비밀번호 찾기는 30분·1회용 링크만 보내고, 링크에서 새 비밀번호를 정할 때만 바뀐다. 토큰은 해시만 저장, 새 링크 발급 시 이전 링크 폐기, 계정당 15분 3회 제한, 메일은 저장 트랜잭션 뒤 비동기 발송. 응답은 계정 존재·횟수 초과와 관계없이 같다. 인증번호(6자리) 방식은 입력 시도 제한을 따로 관리해야 해서 택하지 않았다.
- **Consequences**: 로그아웃은 여전히 클라이언트 토큰 삭제뿐이다(필요하면 같은 버전 증가로 "모든 기기 로그아웃" 가능). 임시 비밀번호 강제 변경 흐름은 호출하는 곳 없이 남아 있다(관리자 강제 초기화 용도). 요청 제한은 계정 단위이며 IP 단위 제한은 없다.

