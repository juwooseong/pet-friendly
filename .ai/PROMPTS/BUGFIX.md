# Prompt Template: Bugfix (`BUGFIX.md`)

## 1. 목적 (Purpose)
시스템 런타임 오류, 매칭 알고리즘 오동작, 또는 UI 결함 발생 시 실시간 로그(`{{ERROR_LOG}}`) 및 현상을 근본 원인(Root Cause) 수준까지 분석하여 증상 덮기(Symptom Patching) 없는 근본적 수정 코드를 제공하는 템플릿입니다.

## 2. 사용할 Agent (Target Agent)
- **Backend Error**: `backend-agent`
- **Frontend Error**: `frontend-agent`
- **Algorithm / Arch Error**: `architect-agent`

## 3. 입력 문서 (Input Documents)
- `.ai/AGENTS.md`
- 발생한 전체 스택트레이스 또는 로그 (`{{ERROR_LOG}}`)
- `docs/DOMAIN.md` (스마트 매칭 계산 규칙)
- `docs/RULES.md` (컨벤션 가이드)

## 4. 수행 절차 (Execution Steps)
1. **로그 및 스택트레이스 분석**: 전체 로그를 수집하여 예외 발생 클래스, 메서드 및 원인 코드 라인 추적.
2. **근본 원인 (Root Cause) 규명**: Null dereference, PostGIS 쿼리 구문 오류, TypeScript 타입 불일치, 매칭 조건 엣지케이스 오동작 여부 진단.
3. **재현 테스트 코드 작성**: 실패하는 단위 테스트(`@Test` 또는 Vitest)를 먼저 작성하여 버그 현상 재현.
4. **소프트웨어 계약 준수 수정**: `docs/DOMAIN.md` 도메인 규칙을 훼손하지 않으면서 근본 원인 수정.
5. **검증**: 작성한 재현 테스트가 통과하는지 확인.

## 5. 출력 형식 (Output Format)
```markdown
### 🐛 Bugfix Diagnosis & Resolution Report

#### 1. 버그 개요 및 발생 경로
- **오류 내용**: `NullPointerException` / `PostGIS ST_DWithin Parse Error`
- **발생 위치**: `file:///path/to/file#L42`

#### 2. 근본 원인 (Root Cause)
- 공공데이터포털 수집 장소 중 `max_weight_limit_kg`가 null인 경우 펫 체중 비교 시 Null Safety 디폴트 처리 누락.

#### 3. 수정 내역 (Diff)
- `file:///path/to/file#L40-L46`

#### 4. 검증 결과
- [x] 재현 테스트 통과 확인
- [x] 기존 기능 회귀(Regression) 없음 확인
```

## 6. 금지 사항 (Prohibitions)
- 전체 스택트레이스를 읽지 않고 추측에 기반한 코드 수정 금지.
- 무의미한 `try-catch` 블록으로 예외를 삼키거나(Swallowing Exception) 임의 더미 0/null 값 반환으로 증상 덮기 금지.
- 실패하는 기존 테스트 케이스를 지우거나 주석 처리하여 눈속임 합격 처리 금지.

## 7. Definition of Done (DoD)
- [ ] 버그 재현 테스트 케이스가 작성되었고 성공적으로 통과함.
- [ ] `./gradlew test` 또는 `npm run type-check` 전체 빌드가 회귀 오류 없이 성공함.
