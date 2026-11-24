# Sprint Planning Specification (SPRINTS.md)

이 문서는 `SYSTEM_DESIGN.md`, `docs/ROADMAP.md` 및 `docs/TASKS.md`를 바탕으로 **Sprint 0부터 Sprint 4까지**의 애자일 스프린트 실행 계획을 수립한 마스터 명세서입니다 (v2.0).

---

## 📝 Change Log (변경 이력)

- **2026-11-24**: **Sprint 3 완료 — 디자인 개편·캐릭터 (`TSK-DES-001`~`TSK-FE-031`, ADR-015)** — 브랜드 가이드(`docs/BRAND.md`)와 캐릭터 콩이(웰시코기)·나비(치즈 고양이), 민트 포인트 색·새 로고. 빈 화면·로딩·오류·사진 없는 장소에 캐릭터 그림, 홈 첫 화면 개편(휴대폰 LCP 3.7초 → 1.8초), 로그인·404·장소 상세 동반 조건 말풍선·리뷰 칭찬, 즐겨찾기 하트·칭찬 움직임(동작 줄이기 설정이면 끔), 파비콘·앱 아이콘·공유 미리보기 이미지. 남은 `TSK-DATA-011`(쇼핑 정책 조회 7,073곳)은 매일 03:00 자동 배치로 진행 중.
- **2026-11-23**: **`TSK-QA-004` 소문자 드라이브 경로 테스트 실패 해결** — `npm test`가 실제 경로 표기(`D:\`)로 바꿔 Vitest를 실행. 소문자 경로에서 재현 후 수정 확인(프런트 154/154 통과).
- **2026-11-23**: **`TSK-FE-015` 완료 (15-5 규칙 문서)** — `docs/RULES.md` 1.2에 스토어 구성과 데이터 조회 위치 규칙(공유 데이터는 스토어, 한 화면 전용·일회성 요청은 화면에서 직접) 정리. 문서만 변경.
- **2026-11-23**: **`TSK-FE-015` 15-4 즐겨찾기 스토어** — 즐겨찾기 화면과 장소 상세가 같은 즐겨찾기 상태를 공유(상세에서 바꾼 결과가 바로 반영, 이미 아는 장소는 상태 재요청 생략). 영향 범위 테스트만 실행(type-check, 스토어·상세·즐겨찾기 52건).
- **2026-11-23**: **`TSK-FE-015` 15-3 장소 검색 스토어** — 장소 상세에서 뒤로 돌아오면 검색어·필터·페이지·결과·스크롤 위치가 유지되고, 메뉴로 새로 들어오면 초기화. 영향 범위 테스트만 실행(type-check, 스토어·장소 탐색·라우터 27건).
- **2026-11-23**: **`TSK-FE-015` 스토어 리팩터링 15-1·15-2** — 반려동물 조회·등록·수정·삭제·대표 지정을 `petStore` 액션으로 이동(반려동물 화면, 장소 탐색). 장소 탐색은 이미 받은 목록을 재사용. 로그아웃 시 반려동물 스토어 초기화. 영향 범위 테스트만 실행(type-check, 스토어·반려동물·장소 탐색 33건).
- **2026-11-23**: **`TSK-FE-022` 홈 대시보드 쇼핑 후순위** — 인기·최근 장소(각 10곳)에서도 검색과 같이 쇼핑을 다른 장소 뒤로. 실DB 통합 테스트 1건 추가.
- **2026-11-23**: **P2 타입 정리·즐겨찾기 여부 API (`TSK-FE-014`, `TSK-FE-016`, `TSK-BE-030`)** — 실제 응답과 맞지 않던 프런트 타입(`Place`/`PetPolicy` 삭제, `User`는 로그인 응답과 일치하게 정리). 장소 1곳의 즐겨찾기 여부 API(`GET /favorites/{placeId}`) 추가, 상세 화면이 즐겨찾기 전체 목록을 받지 않게 변경. 영향 범위 테스트만 실행(type-check, 상세 화면 34, 즐겨찾기 컨트롤러).
- **2026-11-23**: **P1 테스트 기반 정비 (`TSK-QA-003`, `TSK-QA-001`, `TSK-QA-002`)** — 백엔드 테스트를 테스트 전용 DB `petspot_test`로 분리(개발 데이터와 격리). 커스텀 쿼리 실DB 테스트 필수 규칙을 `docs/RULES.md`에 수립하고 현재 쿼리 전부 충족 확인. Playwright E2E(`npm run e2e`): 백엔드·프런트를 E2E 전용 DB(`petspot_e2e`)로 직접 띄우고 로그인→검색→리뷰→새로고침→즐겨찾기 흐름 회귀 테스트. 백엔드 279 / 프런트 151 / E2E 1 통과.
- **2026-11-23**: **`TSK-FE-020` 쇼핑 후순위, `TSK-FE-021` 변경금지 이미지 표시 완료 → 9개 화면 전부 완료** — 카테고리를 고르지 않은 검색(장소 탐색·지도)에서 쇼핑을 다른 장소 뒤로(결과 수 유지). 공공누리 제3·4유형 이미지는 모든 화면에서 자르지 않고 원본 비율로 표시(ADR-012 보완). 장소 상세(`TSK-FE-008`) 완료 처리. 백엔드 279 / 프런트 151 tests 통과.
- **2026-11-23**: **`TSK-BE-031` 실내 출입 여부 노출 완료** — 장소 응답에 `isIndoorAllowed` 추가, 상세 화면 동반 조건에 "실내 동반 가능/실내 출입 불가" 배지(정보 없으면 표시 안 함). 백엔드 278 / 프런트 149 tests 통과.
- **2026-11-23**: **`TSK-DATA-011` 정책 조회 순서 변경 및 1차 실행** — 정책 조회를 쇼핑이 아닌 장소(관광지·레포츠·숙박·음식점·문화시설)부터 하도록 바꾸고 800곳 실행(실패 0). 크기 정보가 있는 장소 30곳 → 690곳(대형견 허용 확인 593곳). 남은 쇼핑 아닌 장소 157곳은 다음 실행에서 먼저 처리, 쇼핑 7,861곳은 그 뒤. 백엔드 277 tests 통과.
- **2026-11-20**: **`TSK-FE-019` 크기 필터 미확인 장소 처리 완료** — 크기 필터 시 허용 확인 장소를 먼저, 크기 정보 없는 장소를 "크기 조건 확인 필요" 배지와 함께 뒤에 표시하고, 허용 안 됨이 확인된 장소만 제외(대형견 기준 30건 → 9,624건). "확인된 곳만 보기"(`sizeVerifiedOnly`)로 기존 동작 선택 가능. 같은 날 Windows PostgreSQL 제거에 따라 개발·테스트 DB를 Docker `petspot-db`로 통일(`.env` 연동).
- **2026-11-20**: **`TSK-FE-018` 지도 재검색 완료** — 지도 이동·확대·축소가 끝나면(400ms 디바운스) 보이는 영역 기준(반경 1~50km)으로 기존 검색 API를 다시 호출해 가까운 순 최대 100곳을 마커로 표시. 작은 이동 무시, 늦게 온 응답 무시, 실패 시 기존 마커 유지. 실데이터 응답 약 0.1초. 프런트 145 tests 통과.
- **2026-11-20**: **`TSK-INF-005` Docker Compose 전체 실행 환경 구축 (Sprint 4 항목 선행)** — DB(PostgreSQL 17 + PostGIS)·Redis·백엔드(Spring Boot jar)·프런트(Nginx) 4개 컨테이너를 `docker compose up -d --build`로 실행, `http://localhost` 단일 주소로 서비스(Nginx가 `/api` 프록시). 운영 프로필 `application-prod.yml`과 `.env.example` 추가, 개발 DB 데이터(9,630곳) 복사. Spring Boot 3.2.2 `bootJar`가 Gradle 9.x와 호환되지 않음을 확인하고 wrapper를 8.7로 유지. 상세는 `docs/TASKS.md` 참조.
- **2026-11-19 (후반)**: **전국 공공데이터 적재 및 장소 상세/탐색 개선** — 정책 조회를 포함한 수집 배치(`TSK-DATA-010`)로 전국 9,630곳 적재·정책 812곳 반영, 서비스키 인코딩/노출 버그 수정, 장소 상세 지연 적재(소개·운영시간·부가 정보·사진 갤러리, ADR-010), 카테고리 8종 매핑(ETC 8,663 → 0, ADR-011), 공공누리 출처 표시(ADR-012), 장소 탐색 페이지 중복 수정·썸네일, 카카오맵 길찾기. Sprint 3 남은 작업: 지도 이동 시 재검색(`TSK-FE-018`, P1 상향), 크기 필터 미확인 장소 처리(`TSK-FE-019`), 쇼핑 비중(`TSK-FE-020`), 제3유형 이미지 표시(`TSK-FE-021`). 백엔드 275 / 프런트 131 tests 통과.
- **2026-11-19**: **ADR-009 / `TSK-DATA-009` 완료** — TourAPI `detailPetTour2`(반려동물 동반여행 조회)가 실제로 존재하며 체중·크기·필요 사항 등 반려동물 정책을 제공함을 실제 호출로 확인(10/10곳). `TSK-DATA-002`/`005`의 "정책 필드 없음" 판정과 ADR-007의 전제를 정정. `detailPetTour2` 클라이언트·DTO·정책 텍스트 파서 구현, 추측 필드 정리(`TSK-DATA-007` 해결), 평평한 게이트웨이 오류 처리, 로그의 서비스키 노출 수정. 238 tests 통과(2 skip). 후속: `TSK-DATA-010`(Place 반영).
- **2026-11-19**: Sprint 2 후속 백로그 `TSK-BE-032`~`035` 완료 — 리뷰 없는 장소 평점 0.0 통일(Flyway V8, 프런트 "평가 없음" 표시), 프로필 수정 닉네임 중복 검사(409), 회원가입/비밀번호 변경 비밀번호 규칙 통일(`PasswordPolicy`), 비즈니스 예외 `BusinessException(ErrorCode)` 통일 및 예외 처리기 12개 → 1개. 백엔드 214 tests 통과(2 skip), 프런트 113 tests 통과. 상세는 `docs/TASKS.md` 참조.
- **2026-11-19**: **Sprint 2 완료** — `TSK-BE-022`(전체 흐름 통합 테스트에 장소 평점 생성/수정/삭제 동기화 검증 추가)와 `TSK-BE-023`(Swagger 정리: 보안 스킴 이름 불일치 버그 수정, 엔드포인트별 에러 응답·예시 자동 명세, 생성 API 201 표시, DTO 139개 필드 설명/예시, 문서-실제 동작 일치 검증 테스트) 완료. 부수적으로 잘못된 UUID/JSON 요청이 500으로 응답하던 버그를 400으로 수정. 후속 과제 `TSK-BE-032`~`035` 기록. 같은 날 TourAPI 수집 배치 자동 실행을 기본 비활성화(`TSK-DATA-003-C` 설계 전 의도치 않은 전체 수집 방지). 전체 `./gradlew test`: 198 tests, 실패/에러 0건, 2 skip. 상세는 `docs/TASKS.md` 참조.
- **2026-09-14**: `TSK-DATA-003-B` 완료 — `TSK-DATA-003-A`에서 발견한 좌표 결측 시 `0.0`/`0.0`(Null Island) 대체 저장 문제에 대해, 전략 A(nullable migration)와 전략 B(좌표 없는 장소 적재 제외)를 비교해 **전략 B를 채택**(ADR-008, `docs/DECISIONS.md`). `Place.latitude`/`longitude`/`location`은 계속 `NOT NULL`로 유지(migration 없음), `TourApiPlaceMapper.toEntity()`는 좌표를 파싱할 수 없으면 임의 값 대신 `null`을 반환하도록 수정, `TourDataCollectorService.saveCollectedPlaces()`는 `contentId` 결측과 동일하게 이 경우를 스킵하도록 수정. 검색/지도 API는 전혀 변경하지 않음(좌표는 항상 유효하다는 기존 불변식 유지). 기존 0.0 기대 테스트 2개를 null 반환 기대로 교체, 좌표 없이 작성됐던 기존 정책/체중 테스트 2개에 유효 좌표 보강, 배치 스킵/검색 미노출을 검증하는 테스트 갱신. 전체 `./gradlew test`: 188 tests, 실패/에러 0건, 2 skip(변경 없음). `TSK-DATA-003-C`(전국 초기 적재) 착수의 선행 조건이 충족됨. 상세는 `docs/TASKS.md` 참조.
- **2026-09-14**: `TSK-DATA-003-A` 완료 — TourAPI(`areaBasedList2`, numOfRows=5) 초기 데이터 적재 최소 파이프라인을 신규 코드 추가 없이 기존 `TourDataCollectorService`(`collectPage`/`saveCollectedPlaces`)+`TourApiPlaceMapper`+`PlaceRepository.findByPublicDataId()` 기반 중복 방지를 그대로 재사용해 검증. 실제 로컬 PostgreSQL/PostGIS에 장소 기본정보가 정상 저장되고, 반려동물 정책 5개 필드(`maxWeightLimitKg`/`allowedSizes`/`isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone`)는 DB 재조회 후에도 전부 `null`로 유지됨을 확인(`TSK-DATA-008` 수정이 실제 저장 경로 전체에서 정상 작동함을 실증). 동일 5건 재적재 시 중복 생성되지 않음, contentId 결측 항목은 스킵되고 나머지는 정상 처리됨, 좌표 결측/파싱 실패가 섞여도 배치 전체가 깨지지 않음을 검증. **신규 발견**: `Place.latitude`/`longitude`/`location`이 DB `NOT NULL` 컬럼이라 좌표를 문자 그대로 `null` 저장할 수 없어 기존 `0.0`/`0.0` 대체 정책이 유지됨 — migration 필요 사항으로 `TSK-DATA-003-B`에 이관. 전체 대량 적재는 `TSK-DATA-003-C`로 이관. 오프라인 테스트 4개 + opt-in 실통합 테스트 1개 신규 추가, 전체 `./gradlew test`: 188 tests, 실패/에러 0건, 2 skip(의도된 게이팅). 상세는 `docs/TASKS.md` 참조.
- **2026-09-14**: `TSK-DATA-008` 완료 — `TSK-DATA-006`(ADR-007)의 "정책 정보가 없으면 null로 유지한다" 원칙을 실제 코드에 반영. `TourApiPlaceMapper.toEntity()`가 `isIndoorAllowed=true`/`isOutdoorAllowed=true`/`hasOffLeashZone=false`를 근거 없이 하드코딩하던 것과, `Place` 생성자가 `allowedSizes`/`isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone`에 `null` 입력을 전체 허용 기본값으로 자동 치환하던 구조적 버그를 함께 제거(`maxWeightLimitKg`처럼 null을 그대로 유지하도록 통일). Entity 구조/DB migration은 변경하지 않음(생성자 로직만 최소 수정, 컬럼은 원래도 nullable이었음). 암묵적 기본값에 의존하던 `PlaceSearchSizeAndDistanceIntegrationTest` 기존 케이스를 명시적 값 지정으로 수정, 정책 필드가 null인 장소의 저장/조회/검색 동작(크기 필터에서는 근거 없어 자동 제외됨, 예외는 없음)을 검증하는 테스트 신규 추가, `TourApiPlaceMapperTest`에 null 유지 검증 테스트 2개 추가. 전체 `./gradlew test`: 183 tests, 실패/에러 0건, 1 skip(의도된 게이팅). `TSK-DATA-003`(초기 대량 적재) 착수의 선행 조건이 충족됨. 상세는 `docs/TASKS.md` 참조.
- **2026-09-14**: `TSK-DATA-006` 완료 — PetSpot 반려동물 정책 데이터의 Source of Truth 및 보강 전략을 설계(코드/Entity 변경 없음, 설계·문서화만 수행). TourAPI = 장소 기본정보 Source of Truth, PetSpot(운영자 검증/관리자 입력, 향후 사용자 제보+승인) = 반려동물 정책 데이터(`maxWeightLimitKg`/`allowedSizes`/`isIndoorAllowed`/`isOutdoorAllowed`/`hasOffLeashZone`) Source of Truth로 분리. `null`=미검증/`false`=근거 있는 불가/`true`=근거 있는 가능 3단계 원칙을 채택했으나, 현재 `TourApiPlaceMapper`와 `Place` 생성자가 이 원칙을 위반(정책 필드에 근거 없는 허용 기본값을 자동으로 채움)하고 있음을 실제 코드에서 발견 — 수정은 `TSK-DATA-008`로 이관, `TSK-DATA-003`(초기 대량 적재) 착수 전 선행 필요. `PASS`/`WARN`/`DENY` 판정에서 정책 필드가 하나라도 `null`이면 보수적으로 `WARN` 처리하는 원칙 수립(코드 미구현). `allowedSizes`는 기존 `PetSizeCategory` enum(SMALL/MEDIUM/LARGE) 값을 그대로 사용하고 JSONB 유지를 권장, 향후 정책 근거/검증 이력을 위한 별도 `PlacePolicy` 엔티티 분리를 제안(즉시 구현 아님). 상세는 `docs/DECISIONS.md`의 ADR-007, `docs/TASKS.md` 참조.
- **2026-09-14**: `TSK-DATA-005` 완료 — TourAPI 반려동물 전용 필드 실제 스키마를 `areaBasedList2`/`detailIntro2`/`detailInfo2`/`detailCommon2` 4개 오퍼레이션 실제 호출(contentTypeId 38/쇼핑·32/숙박 교차 검증)로 확인. 기존 코드의 추측 필드(`acmpyTypeCd`/`claAcmpyInfo`/`relaAcmpyInfo`/`relaPosbleFclty`/`relaRqustFclty`/`etcAcmpyInfo`/`acmpyPsblCpam`)와 체중/크기/목줄/이동장/실내외/오프리쉬 관련 필드는 **전부 미존재로 확인됨**. 반려동물 관련 필드는 `detailIntro2`의 `chkpetshopping`(쇼핑 타입 한정, 관찰된 값은 빈 문자열) 하나뿐이라 Place 필드 자동 매핑에는 부족(4개 필드 모두 판정 D/C). `mapx`/`mapy`는 실제로 경도/위도임을 확인. `detailCommon2`는 `defaultYN` 등 옵션 파라미터를 지원하지 않는 점도 확인(공식 문서와 상이). 실제 성공 응답 5건을 fixture로 저장(serviceKey 미포함), 오프라인 검증 테스트 5개 추가. 미존재 확인 필드 정리는 `TSK-DATA-006`으로 이관. 상세는 `docs/TASKS.md` 참조.
- **2026-09-09**: `TSK-DATA-004` 완료(포털 확인 포함 최종 확정) — TourAPI 로컬 프로필 서비스키 활용신청 상태를 data.go.kr 마이페이지(처리상태: 승인, 활용기간 2026-09-09~2028-09-09)와 `areaBasedList2` 실제 호출(2회, 매번 `resultCode 0000` 정상 응답) 양쪽으로 확정. `TSK-DATA-002` 시점의 `SERVICE_KEY_IS_NOT_REGISTERED_ERROR`는 승인 처리 지연이었던 것으로 결론(코드/설정 문제 아님). 실제 성공 응답을 fixture로 저장(`success-area-based-list2-real-2026-09-09.json`, serviceKey 미포함). 반려동물 전용 필드는 이 표준 목록 응답에 없음을 확인했으나 다른 오퍼레이션 확인은 `TSK-DATA-005`로 이관. 상세는 `docs/TASKS.md` 참조.
- **2026-09-09**: `TSK-DATA-002`(Sprint 1 후속 검증) 완료 — 한국관광공사 TourAPI 반려동물 동반여행 서비스 클라이언트를 실제 HTTP 호출로 검증. `application.yml` 잘못된 `baseUrl`(`KorPetTourService` → `KorPetTourService2`)과 존재하지 않는 오퍼레이션(`/detailPetTour1` → `/areaBasedList2`) 버그를 실제 호출 증거로 확인/수정, 게이트웨이 오류 스키마 처리 추가. 로컬 시크릿이 `SERVICE_KEY_IS_NOT_REGISTERED_ERROR`(reasonCode 30)를 반환해 반려동물 전용 필드의 실제 payload 검증은 미완료로 남음(`TSK-DATA-004`/`TSK-DATA-005` 백로그, `docs/TASKS.md` 참조). DB 대량 적재는 범위 외. 상세는 `docs/TASKS.md`의 "2026-09-09 완료 기록" 참조.
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
| **Sprint 2** | Backend Complete | **Review Service/API, My Page API, Dashboard API, E2E Integration Test & Swagger** | **[COMPLETED]** (2026-11-19) |
| **Sprint 3** | Frontend MVP | **Vue 3 + Vite Frontend 9개 화면 구현 & Spring Boot 백엔드 API 실서비스 완전 연동** | **[COMPLETED]** (2026-11-24) |
| **Sprint 4** | Advanced AI & Infra | OpenAI AI 추천, Redis Caching, Elasticsearch, AWS & Docker CI/CD | [PLANNED] |

---

## 🚀 Sprint 2: Backend Feature Completion (백엔드 핵심 기능 완결) [COMPLETED] (2026-11-19)

- **Sprint Goal**: 백엔드 도메인 및 API 레이어 완결 (Review, My Page, Dashboard, Integration Test, Swagger 정리).

### 배정된 핵심 파트
1. **Review Service 및 API 완료**:
   - Review CUD API (`POST`, `GET`, `PUT`, `DELETE /api/v1/reviews`)
   - Review 작성/수정/삭제 시 `Place` 평균 평점 (`rating`) 및 리뷰 수 (`reviewCount`) 실시간 동기화
   - 장소당 1인 1리뷰 409 Conflict 처리 및 Soft Delete 적용
2. **My Page API** ✅ 완료 (2026-08-27 문서 반영 — 실제로는 이전에 이미 구현됨):
   - 실제 엔드포인트는 `GET /api/v1/users/me`(사용자 정보 + 대표 반려동물 + 펫/즐겨찾기/리뷰 카운트 통합 응답). 즐겨찾기/내 리뷰/내 반려동물 "목록"은 각 화면이 기존 전용 API(`GET /users/me/favorites`, `GET /users/me/reviews`, `GET /pets`)를 그대로 재사용하는 구조로, 이 API가 목록까지 함께 내려주지는 않음(중복 응답 방지를 위한 의도적 설계로 확인).
3. **Dashboard API** ✅ 완료 (2026-08-27 문서 반영 — 실제로는 이전에 이미 구현됨):
   - `GET /api/v1/dashboard`. 홈 화면(`HomePage.vue`)에서 이미 연동 중이었음.
4. **Integration Test (통합 테스트)** ✅ 완료 (2026-11-19, `Sprint2FullFlowIntegrationTest` — 장소 평점 동기화 검증 보강):
   - 회원가입 ➔ 로그인 ➔ JWT 발급 ➔ Pet 등록 ➔ Favorite 등록 ➔ Review 작성 ➔ Place Rating 변경 ➔ 조회 전체 Flow 검증 E2E Test.
5. **API Documentation** ✅ 완료 (2026-11-19, 에러 응답/예시 자동 명세 + 문서-실제 일치 검증 테스트):
   - Swagger OpenAPI 3.0 명세 정리, API Tag 분류, Request/Response/Error Example 작성.

> [!NOTE]
> **Refactoring Sprint (Sprint 2 종료 후 진행)**:
> `ST_DWithin` 적용, Pagination 최적화, QueryDSL 개선, Domain/Application Layer 정제, Exception 구조 개편, Performance Optimization.

---

## 🚀 Sprint 3: Frontend MVP (Vue 3 프론트엔드 실서비스 구축) [COMPLETED] (2026-11-24)

- **Sprint Goal**: Vue 3 + Vite + TailwindCSS 기반 프론트엔드 구축 및 Spring Boot 백엔드 API와의 100% 완전한 연동을 통해 실사용 가능한 MVP 서비스 완성.
- **기술 스택**: Vue 3 (Composition API), Vite, Pinia, Vue Router 4, Axios, TailwindCSS, Spring Boot API 연동, JWT Auth, 반응형 UI.
- **진행 현황 (2026-11-24 완료)**: 9개 화면과 확장 스코프, 디자인 개편·캐릭터까지 완료. 상세 Task 체크리스트는 `docs/TASKS.md`의 Sprint 3 섹션 참조.
- **스코프 확장**: 원래 9개 화면 계획에 더해 계정 복구(아이디/비밀번호 찾기, 임시 비밀번호 강제 변경) 기능이 Sprint 3 범위로 추가되어 완료됨.

### Frontend 설계 문서 수립 항목
- [x] Frontend Architecture Spec / Common Layout / UI 컴포넌트 7종
- [x] Vue Router 4 Routing Specification (`/map` 라우트 포함 전체 등록 완료, 2026-08-27)
- [x] Pinia Global Store 기본 구조 (`authStore`, `petStore`, `placeStore`) + `favoriteStore`, 스토어 경유 데이터 조회 (`TSK-FE-015`, 2026-11-23 완료)
- [x] Axios Interceptor & Auth Token Manager (401 처리, 에러 메시지 추출)
- [ ] Component Tree Taxonomy / API Endpoint Mapping Table 문서화

### 9개 구현 화면 명세 및 진행 상태
1. **로그인 (`/login`)** ✅ 완료: JWT 저장, LocalStorage 관리, 자동 로그인, 로그아웃, CORS/에러 필드 버그 수정 완료, 로그인 응답 `user` 누락 크래시 버그 수정, 아이디/비밀번호 찾기 이동 버튼 연결
2. **회원가입 (`/signup`)** ✅ 사실상 완료: Form Validation, 비밀번호 확인/패턴 검증, 필수값 자동 포커스, Caps Lock 감지, 비밀번호 한글 입력 차단, 이메일/닉네임 중복 시 인라인 에러 표시 완료 / 실시간 중복 확인만 미구현
3. **홈 (`/`)** ✅ 완료: 추천 장소, 인기 장소, 최근 등록 장소, 대표 반려동물 카드 렌더링 (Dashboard API 연동)
4. **장소 검색 (`/places`)** ✅ 완료 (2026-08-14, 2026-11-23 보강): 키워드 검색, 카테고리 필터, 반려동물 크기 조건, 거리순 정렬(위치 기반) 전부 연동 완료. 크기 미확인 장소 뒤에 포함(`TSK-FE-019`), 카테고리 미선택 시 쇼핑 후순위(`TSK-FE-020`)
5. **지도 (`/map`)** ✅ 완료 (2026-11-20): Kakao Map 렌더링·장소 마커·카드·상세 이동(`TSK-FE-017`), 지도 이동/확대·축소 시 보이는 영역 기준 재검색(`TSK-FE-018`, 가까운 순 최대 100곳)
6. **장소 상세 (`/places/{id}`)** ✅ 완료 (2026-11-23): 기본 정보, 즐겨찾기, 리뷰, 사진 갤러리, 장소 소개, 운영 시간, 반려동물 동반 조건 배지·편의시설, 주차·문의처·홈페이지, 카카오맵 길찾기, 공공누리 출처 표시. 실내 출입 여부 배지(`TSK-BE-031`), 변경금지(제3유형) 사진 자르지 않고 표시(`TSK-FE-021`)
7. **즐겨찾기 (`/favorites`)** ✅ 완료 (2026-08-27): 목록 조회(이미지/카테고리/평점·리뷰 수 포함), 장소 상세 이동, 삭제(중복 요청 방지) 전부 연동 완료
8. **내 반려동물 (`/pets`)** ✅ 완료 (2026-08-27): 목록 조회, 등록/수정/삭제, 대표 반려동물 지정 전부 연동 완료 (`sizeCategory`는 서버 계산값 표시 전용)
9. **마이페이지 (`/mypage`)** ✅ 완료 (2026-08-27): 내 정보(이메일/닉네임/프로필 이미지)·대표 반려동물·통계 표시, 회원 정보 수정, 즐겨찾기/반려동물/리뷰 메뉴 이동 전부 연동 완료(리뷰는 신규 `MyReviewsPage.vue`로 연결, 기존 백엔드 API 재사용)

### 계획 외 추가 구현: 계정 복구 (Account Recovery) [COMPLETED]
- **아이디 찾기 (`/find-id`)**: 닉네임 → 마스킹된 이메일 조회 (`POST /api/v1/auth/find-id`)
- **비밀번호 찾기 (`/find-password`)**: 이메일 → 임시 비밀번호 발급/발송, 계정 존재 여부 비노출 (`POST /api/v1/auth/find-password`)
- **강제 비밀번호 변경**: 임시 비밀번호 로그인 시 `requiresPasswordChange=true` 응답, 변경 전까지 일반 API 접근 차단, `PATCH /api/v1/auth/password`로 변경
- **닉네임 중복 검증**: 회원가입 시 이메일과 동일하게 닉네임 중복도 409로 차단

---

## 🚀 Sprint 4: Advanced Features, AI & Infra (고급 기능 & 인프라) [PLANNED]

- **Sprint Goal**: AI 추천 알고리즘 및 캐싱/인프라 고도화.
- **주요 구현 항목**:
  - OpenAI API 기반 맞춤형 AI 장소 추천 파이프라인
  - Redis Caching (장소 검색 및 JWT 세션)
  - Elasticsearch 전문 검색 도입
  - ~~Docker Compose Production 환경~~ ✅ 선행 완료 (2026-11-20, `TSK-INF-005`), AWS 배포 및 CI/CD 파이프라인
