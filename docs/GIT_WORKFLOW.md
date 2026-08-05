# Git Workflow & AI Git Usage Guidelines

이 문서에서는 `pet-friendly` 프로젝트의 브랜치 관리 전략, 커밋 메시지 작성 규칙, PR 및 Merge 전략, 배포/태그 규칙, 그리고 **AI 커밋 및 가이드라인**에 대해 설명합니다.

---

## 1. 브랜치 전략 (Branching Strategy)

기본적으로 **GitHub Flow / Simplified Git Flow** 형태를 지향합니다.

* `main` (또는 `master`): 제품 서비스로 출시될 수 있는 안정적인 상태의 코드를 관리하는 보호된 브랜치 (Protected Branch)입니다.
* `develop` (선택 사항): 다음 버전을 위해 통합 개발이 진행되는 브랜치입니다.
* `feature/<기능명>` 또는 `feature/<issue-id>-<기능명>`: 새로운 기능 개발을 진행하는 브랜치입니다.
* `fix/<버그명>` 또는 `bugfix/<issue-id>-<버그명>`: 버그 수정을 위한 브랜치입니다.
* `hotfix/<긴급수정명>`: 운영 환경(main)의 긴급한 버그를 해결하기 위한 브랜치입니다.
* `chore/<작업명>`: 빌드 설정, 패키지 매니저 관리 등 기타 작업을 위한 브랜치입니다.

---

## 2. 커밋 메시지 규칙 (Conventional Commits)

커밋 메시지는 [Conventional Commits 1.0.0](https://www.conventionalcommits.org/) 사양을 준수하여 작성합니다.

### 기본 구조
```text
<type>(<scope>): <short summary>

[optional body]

[optional footer(s)]
```

### Commit Types
* `feat`: 새로운 기능 추가
* `fix`: 버그 수정
* `docs`: 문서 수정 (예: README, 주석, GIT_WORKFLOW 등)
* `style`: 코드 포맷팅, 세미콜론 누락 등 (코드의 로직 변경 없음)
* `refactor`: 코드 리팩토링 (기능 변경이나 버그 수정 없음)
* `test`: 테스트 코드 추가 또는 수정
* `chore`: 빌드 업무 수정, 패키지 매니저 설정 등 (src 또는 test 코드 수정 없음)
* `ci`: CI 구성 파일 및 스크립트 변경
* `perf`: 성능 향상을 위한 코드 변경

### 예시
```text
feat(auth): 카카오 소셜 로그인 기능 구현

- OAuth2.0 카카오 인증 흐름 추가
- 사용자 토큰 발급 및 로컬 스토리지 저장 로직 작성

Closes #12
```

---

## 3. PR 규칙 (Pull Request Rules)

1. **Self-Review 진행**: PR 생성 전 본인의 변경 사항 및 Diff를 스스로 검토합니다.
2. **템플릿 준수**: PR 본문에 변경 이유, 관련 이슈 번호, 검증 방법(테스트 방법)을 명시합니다.
3. **CI/CD 검증 통과**: 관련 빌드, 린트(Lint), 단위 테스트가 모두 통과해야 합니다.
4. **리뷰어 승인 (Code Review)**: 최소 1명 이상의 리뷰어 승인(Approve)을 득한 후 Merge 합니다.
5. **작은 단위 PR**: 하나의 PR은 가능한 하나의 목표(기능/버그 수정)에 집중하고 거대한 변경을 지양합니다.

---

## 4. Merge 전략 (Merge Strategy)

프로젝트 히스토리 가독성과 추적 용이성을 위해 다음과 같은 Merge 전략을 권장합니다.

* **Feature/Fix -> Main/Develop (Squash and Merge)**
  * 여러 개의 자잘한 커밋들을 하나로 합쳐서 메인 브랜치에 단일 커밋으로 깔끔하게 남깁니다.
  * PR 제목을 최종 커밋 메시지로 활용합니다.
* **Rebase and Merge** (필요시)
  * 커밋 히스토리를 선형으로 유지하고자 할 때 사용합니다.
* **Merge Commit (Create a Merge Commit)** (Release/Hotfix 시)
  * 통합 시점 및 전체 브랜치 흐름 기록이 필요한 경우에 한해 사용합니다.

---

## 5. Tag & Release 규칙 (Semantic Versioning)

버전 관리는 [Semantic Versioning 2.0.0 (SemVer)](https://semver.org/)을 따릅니다.

* **버전 형식**: `vX.Y.Z` (예: `v1.0.0`, `v1.2.3`)
  * `X` (MAJOR): 기존 버전과 호환되지 않는 큰 API 변경이 있을 때
  * `Y` (MINOR): 하위 호환성을 유지하면서 새로운 기능을 추가할 때
  * `Z` (PATCH): 하위 호환성을 유지하면서 버그를 수정할 때
* **Tag 생성 및 Release 문서 작성**
  * `main` 브랜치에 배포 준비가 완료되면 Git Tag를 생성합니다 (`git tag -a v1.0.0 -m "Release v1.0.0"`).
  * GitHub Release를 통해 주요 변경 사항(Changelog)을 정리하여 작성합니다.

---

## 6. AI 사용 시 Git 작업 규칙 (AI Git Rules)

AI(Assistant)가 프로젝트 내에서 작업을 수행하거나 커밋을 만들 때 준수해야 하는 규칙입니다.

1. **`main` / `master` 브랜치 직접 커밋 금지**:
   * AI는 절대 보호된 브랜치(`main`, `master`)에 직접 커밋하거나 push하지 않습니다.
   * 작업 시 전용 feature 브랜치(예: `feature/ai-<작업명>`)를 새로 생성하여 작업합니다.
2. **커밋 전 자체 검증 mandatory**:
   * 코드 수정 완료 후, build, test, lint 명령어를 실행하여 문제가 없는지 실질적인 검증을 완료한 후 커밋을 작성합니다.
3. **명확하고 정확한 커밋 메시지 작성**:
   * AI가 생성하는 커밋도 Conventional Commits 규칙을 엄격히 준수합니다.
   * 변경된 이유와 내용을 명확히 기술합니다.
4. **파괴적 Git 명령어 금지**:
   * `git reset --hard`, `git push --force`, `git clean -fd` 등 작업 내역이나 원격 저장소를 훼손할 수 있는 명령어는 사용자 승인 없이 실행하지 않습니다.
5. **작은 작업 단위 커밋**:
   * 한 번에 과도하게 많은 파일을 변경하지 않고, 논리적인 단위로 나누어 작업 및 커밋을 진행합니다.
