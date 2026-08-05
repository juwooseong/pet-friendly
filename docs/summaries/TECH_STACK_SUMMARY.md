# Context Summary: Tech Stack & Architecture (`TECH_STACK_SUMMARY.md`)

> **Note**: 이 문서는 `docs/summaries/CONTEXT_SUMMARY_GUIDE.md` 기준에 따라 아키텍처 및 기술 스택 엑기스만을 요약한 빠른 참조 가이드입니다.

---

## 1. Technical Stack Core Summary

- **Frontend**: Vue 3.x, TypeScript 5.x (Strict, No `any`), Vite, Pinia, Vue Router 4, Leaflet.js (`@types/leaflet`)
- **Backend**: Spring Boot 3.2+, Java 17/21, Spring Data JPA, QueryDSL, Spring Security + JWT, Spring Batch (`@Scheduled`)
- **Database & Spatial**: PostgreSQL 15+, PostGIS `GEOMETRY(Point, 4326)`, Spatial Indexing (`ST_DWithin`)
- **Caching**: Redis Cluster (Spring Cache `@Cacheable`)

---

## 2. NFR Targets & Responsive Layout Summary

- **Spatial Query Latency**: 위치 기반 반경 검색 **P95 < 50ms**, **P99 < 120ms**
- **Availability Target**: SLA **99.99%** (공공데이터 외부 API 다운 시 Local PostGIS Fallback)
- **Responsive Layout**: Desktop (`≥901px`) 460px Split View / Mobile (`≤900px`) Bottom Nav + Drawer
