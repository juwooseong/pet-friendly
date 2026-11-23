# Development Rules & Code Conventions (RULES.md)

## 1. Frontend Rules (Vue 3 + TypeScript)

### 1.1 TypeScript Strict Type Policy
- **Strict Mode Enabled**: `tsconfig.json`에서 `"strict": true`를 필수 설정한다.
- **No `any` Policy**: `any` 타입 사용을 금지하며, 불분명한 인풋은 `unknown` 타입 사용 후 Type Narrowing(타입 좁히기)을 수행한다.
- **Type Location**: 공통 프론트엔드 모델 타입 인터페이스는 `@/types/` 하위 디렉토리에 전용 모듈(`user.ts`, `place.ts`, `pet.ts`)로 분리한다.

### 1.2 Vue 3 Component & Composition API Standards
- **SFC (Single File Component)**: 모든 Vue 컴포넌트는 `<script setup lang="ts">` 구문을 사용한다.
- **Props & Emits Typing**:
  ```vue
  <script setup lang="ts">
  import type { Place, MatchResult } from '@/types/place';
  
  defineProps<{
    place: Place;
    matchResult: MatchResult;
  }>();
  
  const emit = defineEmits<{
    (e: 'selectPlace', place: Place): void;
  }>();
  </script>
  ```
- **State Management (Pinia Typed Stores)**:
  - `useAuthStore`: `User` 세션 및 JWT 관리
  - `usePetStore`: `Pet[]` 펫 목록 CRUD 및 `activePetId` 상태 관리
  - `usePlaceStore`: `PlaceFilter`, `Place[]` 리스트 및 장소 캐시

---

## 2. Backend Rules (Spring Boot 3)

### 2.1 Package Architecture (Layered Architecture)
```
com.petspot
├── domain
│   ├── user (Entity, Repository, Service, Controller, DTO)
│   ├── pet (Entity, Repository, Service, Controller, DTO)
│   ├── place (Entity, Repository, Service, Controller, DTO)
│   └── review (Entity, Repository, Service, Controller, DTO)
├── global
│   ├── config (SecurityConfig, RedisConfig, WebConfig)
│   ├── error (GlobalExceptionHandler, ErrorCode)
│   └── util (JwtTokenProvider, GeometryUtils)
└── infrastructure
    └── publicdata (PublicDataBatchScheduler, TourApiClient)
```

### 2.2 Entity & DTO Conventions
- **Immutability & Builders**: Entity 클래스에는 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용하며, Entity 변경 시 의미 있는 도메인 메서드 사용 (Setter 사용 금지).
- **Request / Response DTO**: Controller 레이어와 Domain Layer 간 직접적인 Entity 노출 절대 금지 (항상 DTO로 매핑).
- **Null Safety & Validation**: Request DTO 필드는 `@NotNull`, `@NotBlank`, `@Size`, `@Positive` validation 어노테이션 명시.

### 2.3 Testing Rules (TSK-QA-001, TSK-QA-003)
- **테스트 DB**: 백엔드 테스트는 실제 데이터가 있는 `petspot_db`가 아니라 같은 `petspot-db` 컨테이너의 **`petspot_test`**를 쓴다(`src/test/resources/config/application.yml`, IDE 실행에도 적용). 다른 DB를 쓰려면 `TEST_DB_URL` 환경변수로 지정한다. 스키마는 테스트 시작 시 Flyway가 만든다.
  - 새 볼륨은 `docker/postgres/init-postgis.sql`이 만들고, 기존 볼륨에는 한 번 직접 만든다: `docker exec petspot-db psql -U petspot -d postgres -c "CREATE DATABASE petspot_test"`
- **실DB 통합 테스트 필수 대상**: 아래 쿼리는 Mockito Mock 테스트만으로는 검증된 것으로 보지 않는다. 실제 PostgreSQL/PostGIS에서 실행되는 `@SpringBootTest` + `@Transactional` 테스트를 **최소 1개** 둔다.
  - QueryDSL 커스텀 쿼리(`*RepositoryImpl`), 특히 `Expressions.*Template`로 쓴 DB 함수(PostGIS `ST_*`, `jsonb_*`, `cast` 등)
  - `@Query`(JPQL/네이티브) 쿼리, 정렬 규칙(`CASE` 정렬 등)이 결과 순서를 바꾸는 쿼리
  - 배경: Sprint 1의 `ST_DWithin`/`ST_Distance`가 Mock 테스트만 통과한 채 실제로는 Hibernate 파싱 오류로 전혀 동작하지 않았다(2026-08-14 발견).
  - 참고 패턴: `PlaceSearchSizeAndDistanceIntegrationTest`(공간·JSONB·정렬), `TourPetPolicyIngestionIntegrationTest`(`@Query` 정렬).
- **테스트 데이터 격리**: 테스트 데이터는 테스트 트랜잭션 안에서 만들고 롤백한다. 위치 검색 테스트는 실제 장소가 없는 서해 바다 좌표(34.00N, 124.52E 부근)와 고유 접두어(UUID) 키워드로 범위를 한정해, 다른 테스트가 남긴 데이터와 섞이지 않게 한다.
- **Spring Data 파생 쿼리**(`findByXxx`)는 애플리케이션 시작 시 검증되므로 별도 실DB 테스트를 의무화하지 않는다.
- **E2E (TSK-QA-002)**: `frontend`에서 `npm run e2e`. Playwright가 백엔드(8090, DB `petspot_e2e`)와 Vite(3100)를 직접 띄우고, 실행마다 E2E DB를 비운 뒤 시드를 넣는다(`frontend/e2e/global-setup.ts`). Docker의 `petspot-db`만 실행 중이면 된다. E2E 파일은 Vitest와 섞이지 않도록 `*.e2e.ts`로 만든다. 화면 흐름(로그인·검색·리뷰·즐겨찾기 등)을 바꾸면 `e2e/review-flow.e2e.ts`를 함께 갱신한다.

---

## 3. API Response Standard (REST Contract)

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-08-04T14:57:00Z"
}
```
