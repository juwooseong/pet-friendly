# Prompt Template: Review Task (`REVIEW_TASK.md`)

## 1. 목적 (Purpose)
구현 완료된 Task(`{{TASK_ID}}`)의 소스 코드가 아키텍처 규칙(`docs/RULES.md`), 도메인 로직(`docs/DOMAIN.md`), 성능 NFR 및 코드 컨벤션을 준수하고 있는지 독립적으로 검증 및 리뷰하는 템플릿입니다.

## 2. 사용할 Agent (Target Agent)
- `architect-agent` 또는 `qa-agent`

## 3. 입력 문서 (Input Documents)
- `.ai/AGENTS.md`
- `docs/RULES.md`
- `docs/DOMAIN.md`
- `docs/TECH_STACK.md`
- 리뷰 대상 구현 소스 코드 파일들

## 4. 수행 절차 (Execution Steps)
1. **아키텍처 및 패키지 구조 검증**:
   - Backend: Layered Architecture 패키지 분리 (`domain/`, `global/`, `infrastructure/`) 지켜졌는지 확인.
   - Frontend: SFC `<script setup lang="ts">`, Pinia Store 및 `@/types/` 스키마 사용 여부 확인.
2. **보안 및 컨벤션 검사**:
   - Controller의 Entity 직접 반환 여부, `@Setter` 사용 여부 점검.
   - Frontend TypeScript `any` 타입 및 Hex 색상 하드코딩 여부 점검.
3. **성능 및 PostGIS 쿼리 검사**:
   - 반경 거리 검색 시 PostGIS Spatial Index (`GEOMETRY(Point, 4326)`) 활용 여부 확인.
4. **리뷰 피드백 작성**: 개선 필요 항목(Must Fix / Recommended)과 잘된 점(Good) 정리.

## 5. 출력 형식 (Output Format)
```markdown
### 🔍 Task Code Review Report: {{TASK_ID}}

#### 1. 리뷰 결과: [PASS / REJECT]

#### 2. 주요 체크리스트
- [ ] 패키지 & 컴포넌트 구조 준수 여부
- [ ] DTO 반환 및 Entity `@Setter` 누락 여부
- [ ] TypeScript strict typing (No `any`) 준수 여부
- [ ] Spatial Index 쿼리 최적화 여부

#### 3. 개선 요청 사항 (Action Items)
- **[Must Fix]**: `file:///path/to/file#L20` - DTO 변환 없이 Entity 노출됨.
- **[Recommended]**: `file:///path/to/file#L45` - Pinia 액션 비동기 에러 핸들링 추가 권장.
```

## 6. 금지 사항 (Prohibitions)
- 컴파일 에러나 빌드 경고가 발생하는 코드를 심사 없이 `PASS` 승인 금지.
- NFR 기준(쿼리 Latency P95 < 50ms)을 충족하지 못하는 쿼리를 묵인하는 행위 금지.

## 7. Definition of Done (DoD)
- [ ] 리뷰 결과 리포트에 작성된 모든 [Must Fix] 항목이 수정 완료됨.
- [ ] `./gradlew test` 및 `vue-tsc` 결과 빌드 에러 없음.
