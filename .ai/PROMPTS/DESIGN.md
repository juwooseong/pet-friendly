# Prompt Template: System Architecture & DB Design (`DESIGN.md`)

## 1. 목적 (Purpose)
신규 기능 추가 또는 변경 사항 발생 시 데이터베이스 스키마(PostgreSQL + PostGIS DDL), REST API 계약(OpenAPI Spec), 도메인 인터페이스(`@/types/`), 또는 시스템 구조를 설계 및 검증하고 `SYSTEM_DESIGN.md` 및 `docs/` 문서를 갱신하는 템플릿입니다.

## 2. 사용할 Agent (Target Agent)
- `architect-agent`

## 3. 입력 문서 (Input Documents)
- `.ai/AGENTS.md`
- `SYSTEM_DESIGN.md`
- `docs/PRODUCT.md`
- `docs/DOMAIN.md`
- `docs/TECH_STACK.md`
- `docs/DECISIONS.md`

## 4. 수행 절차 (Execution Steps)
1. **요구사항 및 하위 호환성 분석**: 변경하려는 아키텍처/DB 스키마가 기존 시스템 및 API 계약과의 하위 호환성을 유지하는지 분석.
2. **도메인 & ERD 스키마 작성**:
   - Backend: DDL SQL / PostGIS Geometry 타입 정의.
   - Frontend: TypeScript Interface 타입 정의.
3. **ADR 작성 (`docs/DECISIONS.md`)**: 아키텍처 결정 배경, 선택 및 트레이드오프 기록 (ADR-XXX).
4. **마스터 문서 갱신**: `SYSTEM_DESIGN.md` 및 `docs/` 하위 파일 업데이트.
5. **사용자 승인 요청**: 변경 설계안을 사용자에게 제시하고 승인 획득.

## 5. 출력 형식 (Output Format)
```markdown
### 📐 System Design & Architecture Proposal: {{FEATURE_NAME}}

#### 1. 설계 변경 배경 및 목표
- 공공데이터포털 동반 장소 내 '실시간 영업 상태' 컬럼 추가에 따른 DDL 및 REST API 확장.

#### 2. DDL & TypeScript Interface 설계안
```sql
ALTER TABLE places ADD COLUMN current_status VARCHAR(20) DEFAULT 'OPEN';
```
```typescript
export type PlaceStatus = 'OPEN' | 'CLOSED' | 'BREAK_TIME';
```

#### 3. ADR 기록 요약 (`docs/DECISIONS.md`)
- **ADR-006**: 실시간 영업 상태 컬럼 추가 및 캐싱 파이프라인 확장.

#### 4. 사용자 검증 체크리스트
- [ ] 하위 호환성 유지 확인
- [ ] 사용자 승인 및 `SYSTEM_DESIGN.md` 반영 완료
```

## 6. 금지 사항 (Prohibitions)
- 사전 설계 검증 및 사용자 승인 없이 파괴적 DB 스키마 변경(Drop/Rename) 실행 금지.
- `docs/DECISIONS.md` 내 ADR 문서화 없는 임의 아키텍처 변경 금지.

## 7. Definition of Done (DoD)
- [ ] `docs/DECISIONS.md` ADR 및 `SYSTEM_DESIGN.md` 업데이트 완료.
- [ ] 사용자 검증 및 최종 승인 획득.
