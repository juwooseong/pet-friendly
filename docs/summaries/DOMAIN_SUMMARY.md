# Context Summary: Domain & Business Logic (`DOMAIN_SUMMARY.md`)

> **Note**: 이 문서는 `docs/summaries/CONTEXT_SUMMARY_GUIDE.md` 기준에 따라 도메인 모델 및 펫 매칭 로직의 엑기스만을 요약한 빠른 참조 가이드입니다.

---

## 1. Domain Entities Summary

| Entity / Interface | Description | Core Attributes |
| :--- | :--- | :--- |
| **User** | 시스템 회원 정보 | `id`, `email`, `nickname`, `pets` |
| **Pet** | 회원의 반려동물 (체중별 소/중/대형 분류) | `id`, `name`, `weightKg`, `sizeCategory` (SMALL/MEDIUM/LARGE) |
| **Place** | 공공데이터 기반 펫 동반 장소 | `id`, `name`, `category`, `address`, `location` (Point 4326), `petPolicy` |
| **PetPolicy** | 장소별 동반 수칙 규정 | `maxWeightLimitKg`, `allowedSizes`, `isIndoorAllowed`, `policyChecklist` |

---

## 2. Smart Pet-Matching Algorithm Rules Summary

1. **`DENY` (부적합)**: `pet.weightKg > place.maxWeightLimitKg`
2. **`WARN` (주의)**: `pet.sizeCategory NOT IN place.allowedSizes` OR 접종 미완료 상태에서 `VACCINE_REQUIRED` 조건 포함 시
3. **`PASS` (적합)**: 상기 조건에 걸리지 않은 모든 정상 동반 가능 케이스
