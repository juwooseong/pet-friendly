# Prompt Template: Implement Task (`IMPLEMENT_TASK.md`)

## 1. 목적 (Purpose)
`docs/TASKS.md`에 명세된 특정 Task ID(`{{TASK_ID}}`)를 Spring Boot 3 또는 Vue 3 + TypeScript 아키텍처 규칙에 맞춰 2시간 이내에 완성 가능한 단위로 신규 개발 및 구현하는 재사용 템플릿입니다.

## 2. 사용할 Agent (Target Agent)
- **Backend Task**: `backend-agent`
- **Frontend Task**: `frontend-agent`

## 3. 입력 문서 (Input Documents)
- `.ai/AGENTS.md`
- `docs/TASKS.md` (대상 Task ID: `{{TASK_ID}}`)
- `docs/RULES.md`
- `docs/DOMAIN.md`
- `docs/TECH_STACK.md`

## 4. 수행 절차 (Execution Steps)
1. **Task 명세 및 DoD 확인**: `docs/TASKS.md`에서 `{{TASK_ID}}`의 선행 작업(Dependencies)이 완료되었는지 확인하고 DoD 항목을 정독한다.
2. **타입/도메인 정의 확인**:
   - Backend: `docs/DOMAIN.md`의 Java 엔티티 및 DTO 스펙 준수.
   - Frontend: `@/types/` 하위 TypeScript Interface 선언 및 strict typing 적용.
3. **코드 구현**:
   - Backend: Controller ➔ Service ➔ Repository (QueryDSL/PostGIS) 순으로 구현. Entity 직접 노출 금지.
   - Frontend: Composition API (`<script setup lang="ts">`) + Pinia Store + Scoped CSS 구현.
4. **검증 실행**:
   - Backend: `./gradlew test --tests {{TEST_CLASS}}` 실행.
   - Frontend: `npm run type-check` (`vue-tsc`) 실행.
5. **DoD 달성 체크**: `docs/TASKS.md` 내 해당 Task 완료 체크(`[x]`) 업데이트.

## 5. 출력 형식 (Output Format)
```markdown
### 🚀 Task Implementation Summary: {{TASK_ID}}

#### 1. 변경 및 생성된 파일
- `[NEW/MODIFY]` `file:///path/to/file`

#### 2. 핵심 구현 내용
- 백엔드: DTO / Service / PostGIS Spatial Repository 및 Controller 엔드포인트 구현
- 프론트엔드: Vue 3 `<script setup lang="ts">` 컴포넌트 및 Pinia Store 연동

#### 3. 검증 결과 (Verification)
- [x] Type Check / Unit Test 실행 결과: PASS
- [x] DoD 만족 여부 확인
```

## 6. 금지 사항 (Prohibitions)
- Backend: Entity 직접 노출 금지, Entity 내 `@Setter` 어노테이션 사용 금지, PostGIS Spatial Index 없이 전체 테이블 Scan 금지.
- Frontend: TypeScript Strict Mode 위반 및 `any` 타입 사용 금지, `css/variables.css` 토큰 무시 임의 Hex코드 작성 금지.

## 7. Definition of Done (DoD)
- [ ] `docs/TASKS.md` 내 `{{TASK_ID}}`에 지정된 DoD 검증 항목 100% 충족.
- [ ] Backend: `./gradlew test` 오류 없음 / Frontend: `npm run type-check` 오류 없음.
