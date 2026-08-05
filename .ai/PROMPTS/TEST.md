# Prompt Template: Test & Quality Verification (`TEST.md`)

## 1. 목적 (Purpose)
PetSpot 서비스의 핵심 기능(스마트 펫 매칭 알고리즘, PostGIS 반경 거리 검색 NFR Latency P95 < 50ms, 대표 펫 전환 UI 뱃지 실시간 변경)에 대해 단위/통합/E2E 테스트 코드를 작성하고 품질을 검증하는 템플릿입니다.

## 2. 사용할 Agent (Target Agent)
- `qa-agent` 및 `backend-agent` / `frontend-agent`

## 3. 입력 문서 (Input Documents)
- `.ai/AGENTS.md`
- `docs/DOMAIN.md` (스마트 펫 매칭 규칙)
- `docs/TECH_STACK.md` (NFR 성능 목표 P95 < 50ms)
- `docs/TASKS.md` (검증 대상 Task ID: `{{TASK_ID}}`)

## 4. 수행 절차 (Execution Steps)
1. **테스트 시나리오 설계**:
   - 백엔드: JUnit 5/Mockito 기반 `evaluatePetMatch` 단위 테스트, PostGIS 위치 반경 `ST_DWithin` Integration Test.
   - 프론트엔드: `evaluatePetMatch.ts` Vitest 테스트, 대표 펫 전환 시 카드 뱃지 리렌더링 E2E 테스트.
2. **성능 / Latency 측정**: PostGIS Spatial Query 반경 쿼리 실행 시간(P95, P99) 100회 실행 로그 측정.
3. **테스트 코드 작성 및 실행**:
   - Backend: `./gradlew test`
   - Frontend: `npm run test`
4. **결과 리포트 및 `docs/TASKS.md` 검증 완료 업데이트**.

## 5. 출력 형식 (Output Format)
```markdown
### 🧪 Test & NFR Verification Report: {{TASK_ID}}

#### 1. 테스트 시나리오 및 실행 결과
- **소형견(초코 4.2kg) 매칭 테스트**: PASS (상세: `🎉 초코(4.2kg) 입장 가능 및 동반 적합 장소입니다!`)
- **대형견(빅터 28.5kg) 매칭 테스트**: DENY (상세: `⛔ 빅터(28.5kg)는 장소 체중 제한(10.0kg)을 초과하여 입장 불가능합니다.`)

#### 2. PostGIS Spatial Query Latency 측정 결과
- **P50**: 12ms / **P95**: 34ms (NFR < 50ms 조건 충족 ✅)
- **P99**: 78ms (NFR < 120ms 조건 충족 ✅)

#### 3. DoD 달성 여부
- [x] 테스트 통과 및 TASKS.md 업데이트 완료
```

## 6. 금지 사항 (Prohibitions)
- 실패하는 경계 조건(Edge Case: 체중 경계값 10.0kg, 접종 여부 null 등) 테스트를 의도적으로 제외하는 행위 금지.
- 실제 데이터베이스 쿼리를 실행하지 않고 Mock 데이터를 통해 쿼리 Latency 수치를 허위 작성하는 행위 금지.

## 7. Definition of Done (DoD)
- [ ] 작성한 모든 단위/통합 테스트 스위트 100% 통과.
- [ ] PostGIS Spatial Query Latency **P95 < 50ms** 검증 완료.
