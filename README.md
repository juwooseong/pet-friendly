# PetSpot (펫스팟) - 공공데이터 기반 펫 프렌들리 동반 지도

> 공공데이터포털 및 한국관광공사(TourAPI) 표준 데이터를 가공하여, 회원 및 회원의 반려동물(체중/견종 크기)에 딱 맞는 동반 장소를 탐색하고 적합도를 실시간 판별해 주는 웹/모바일 반응형 플랫폼입니다.

---

## 🤖 AI 에이전트 지침서 & 프롬프트 라이브러리

- **[.ai/AGENTS.md](.ai/AGENTS.md)**: AI Agent 팀 (`architect`, `backend`, `frontend`, `qa`)의 역할, 금지 사항 & DoD
- **[.ai/PROMPTS/](.ai/PROMPTS/)**: 재사용 작업 템플릿 (`IMPLEMENT_TASK`, `REVIEW_TASK`, `BUGFIX`, `REFACTOR`, `TEST`, `DESIGN`)
- **[docs/SESSION_TEMPLATE.md](docs/SESSION_TEMPLATE.md)**: Task 완료 후 세션 인수인계 리포트 서식

---

## 📚 프로젝트 설계, 로드맵 & 스프린트 문서 (`docs/`)

- **[SYSTEM_DESIGN.md](SYSTEM_DESIGN.md)**: 통합 시스템 설계 및 명세서 (참조 통합 Master Spec)
- **[docs/SPRINTS.md](docs/SPRINTS.md)**: **[신규]** Sprint 0~3 애자일 스프린트 계획 및 DoD 명세서
- **[docs/ROADMAP.md](docs/ROADMAP.md)**: 문서별 역할 및 단계별 실행 로드맵 가이드
- **[docs/PRODUCT.md](docs/PRODUCT.md)**: 제품 비전, PRD, 페르소나 및 User Stories
- **[docs/DOMAIN.md](docs/DOMAIN.md)**: 도메인 엔티티 모델 (TS Types) 및 스마트 펫 매칭 알고리즘 명세
- **[docs/TECH_STACK.md](docs/TECH_STACK.md)**: 기술 스택 선정 (Vue 3 + TS & Spring Boot 3), 아키텍처 다이어그램 및 NFR 성능 목표
- **[docs/RULES.md](docs/RULES.md)**: 코딩 컨벤션 (Strict TS, Package Layout), 반응형 UI/UX 가이드라인 및 디자인 시스템 토큰
- **[docs/TASKS.md](docs/TASKS.md)**: 마일스톤 단계별 개발 및 검증 체크리스트
- **[docs/DECISIONS.md](docs/DECISIONS.md)**: 아키텍처 의사결정 기록 (ADR)
- **[docs/summaries/](docs/summaries/)**: Context Summary 요약본 (`DOMAIN_SUMMARY.md`, `TECH_STACK_SUMMARY.md`, `CONTEXT_SUMMARY_GUIDE.md`)

---

## 📁 프로젝트 구조

```
pet-friendly/
├── .ai/
│   ├── AGENTS.md           # AI Agent 팀 역할, 금지사항 & DoD 명세서
│   └── PROMPTS/            # 재사용 가능한 AI 작업 실행 템플릿 모음
├── SYSTEM_DESIGN.md        # 통합 시스템 설계 & 명세 참조 Master Spec
├── README.md               # 프로젝트 안내 문서
├── docs/                   # 세부 컨텍스트 및 로드맵 문서 모음
│   ├── SPRINTS.md          # Sprint 0~3 애자일 스프린트 실행 계획
│   ├── SESSION_TEMPLATE.md # Task 완료 세션 리포트 서식 (11개 항목)
│   ├── ROADMAP.md          # 문서 역할 및 프로젝트 진행 로드맵 가이드
│   ├── PRODUCT.md          # 제품 기획 및 사용자 정의
│   ├── DOMAIN.md           # 도메인 모델 및 스마트 매칭 로직
│   ├── TECH_STACK.md       # 기술 스택 및 아키텍처
│   ├── RULES.md            # 개발 및 디자인 시스템 규정
│   ├── TASKS.md            # 로드맵 및 검증 상태
│   ├── DECISIONS.md        # 아키텍처 의사결정 기록 (ADR)
│   └── summaries/          # Context Summary 요약본 디렉토리
├── index.html              # 웹/모바일 반응형 메인 HTML
├── css/
│   ├── variables.css       # Design System Tokens
│   ├── style.css           # Core & Layout Styles
│   ├── components.css      # Place Cards, Badges, Modals, Toasts
│   └── pet-profile.css     # User & Pet Profile Styles
└── js/
    ├── mockData.js         # 공공데이터 규격 Sample Data & Initial Demo User
    ├── authService.js      # 회원가입 & 계정 관리 서비스
    ├── petService.js       # 펫 프로필 & 스마트 매칭 알고리즘 엔진
    ├── apiService.js       # 공공데이터 파싱 & 검색/필터링/리뷰 서비스
    ├── mapService.js       # Leaflet.js 인터랙티브 지도 엔진
    └── app.js              # 메인 UI 컨트롤러 & 이벤트 바인딩
```
