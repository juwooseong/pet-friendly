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
