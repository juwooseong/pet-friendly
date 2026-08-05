# Prompt Template: Refactor (`REFACTOR.md`)

## 1. 목적 (Purpose)
기존 동작 기능의 가시적 동작(External Behavior)을 변경하지 않으면서, 코드 가독성, 복잡도, 유지보수성, TypeScript Strict 타입 검사 및 Spring Boot 레이어 구조(`docs/RULES.md`)를 개선하는 재사용 템플릿입니다.

## 2. 사용할 Agent (Target Agent)
- **Backend Code**: `backend-agent`
- **Frontend Code**: `frontend-agent`

## 3. 입력 문서 (Input Documents)
- `.ai/AGENTS.md`
- `docs/RULES.md` (코딩 규정 & 패키지 레이아웃)
- `docs/DOMAIN.md` (도메인 인터페이스)
- 리팩토링 대상 코드 파일 (`{{TARGET_FILE}}`)

## 4. 수행 절차 (Execution Steps)
1. **기존 테스트 검증**: 리팩토링 전 모든 기존 단위 테스트가 정상 통과하는지 확인.
2. **코드 냄새(Code Smell) 진단**:
   - Backend: 거대한 Controller/Service 메서드 분리, Entity 직접 반환 지점 DTO 캡슐화, QueryDSL 중복 쿼리 튜닝.
   - Frontend: 거대한 SFC 컴포넌트 서브 컴포넌트화, Pinia Store 액션 모듈화, `any` 타입 지우기.
3. **단계적 리팩토링 적용**: 기능을 작게 쪼개어 리팩토링 수행.
4. **회원 검증**: 리팩토링 후 기존 테스트 및 추가 가드 테스트 재실행.

## 5. 출력 형식 (Output Format)
```markdown
### ♻️ Code Refactoring Report: {{TARGET_FILE}}

#### 1. 리팩토링 목적 및 개선 대상
- Controller 내 직접 쿼리 로직을 QueryDSL Repository Custom Impl로 이관.
- 프론트엔드 장소 카드 컴포넌트 내 스마트 뱃지 시각화 로직을 `SmartMatchTag.vue`로 독립 분리.

#### 2. 주요 변경 사항
- `[MODIFY]` `file:///path/to/file`

#### 3. 검증 결과
- [x] 기존 기능 동작 100% 동일 유지
- [x] 전체 테스트 스위트 통과 확인
```

## 6. 금지 사항 (Prohibitions)
- 리팩토링을 빙자하여 외부 API 사양(REST Contract)이나 도메인 매칭 로직(`PASS`/`WARN`/`DENY`)을 임의 변경하는 행위 금지.
- 리팩토링 도중 신규 기능을 추가하거나 관련 없는 코드를 한꺼번에 섞어서 수정하는 행위 금지.

## 7. Definition of Done (DoD)
- [ ] 코드 복잡도(Cyclomatic Complexity)가 감소하였거나 캡슐화가 개선됨.
- [ ] 빌드 및 전체 테스트 통과 (`./gradlew test` & `npm run type-check`).
