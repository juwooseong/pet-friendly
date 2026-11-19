-- Flyway V9 (TSK-DATA-010, ADR-009): TourAPI detailPetTour2 반려동물 정책 동기화 메타데이터
-- pet_policy_detail : Place 전용 컬럼이 없는 해석 결과(동반 구역, 목줄/이동장/예방접종 필요 여부, 마릿수 등)와 원문 텍스트
-- pet_policy_source : 정책 값의 출처 (TOURAPI = detailPetTour2 자동 반영, ADMIN = 관리자 검증 값 - 재동기화로 덮어쓰지 않음)
-- pet_policy_synced_at : 마지막으로 TourAPI 정책을 조회한 시각 (NULL = 아직 조회 전, 배치가 우선 처리)
ALTER TABLE places ADD COLUMN pet_policy_detail JSONB;
ALTER TABLE places ADD COLUMN pet_policy_source VARCHAR(20);
ALTER TABLE places ADD COLUMN pet_policy_synced_at TIMESTAMP WITH TIME ZONE;

-- 정책 조회 대기 장소를 빠르게 찾기 위한 부분 인덱스
CREATE INDEX idx_places_pet_policy_pending ON places (created_at)
    WHERE public_data_id IS NOT NULL AND pet_policy_synced_at IS NULL;
