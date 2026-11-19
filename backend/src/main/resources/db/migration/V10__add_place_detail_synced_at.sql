-- Flyway V10: 장소 상세 정보(TourAPI detailCommon2 개요 / detailIntro2 운영시간) 조회 시각
-- 상세 화면을 처음 열 때 한 번만 TourAPI를 호출해 description/operating_hours를 채우고, 이후에는 저장된 값을 쓴다.
-- NULL = 아직 조회 전
ALTER TABLE places ADD COLUMN detail_synced_at TIMESTAMP WITH TIME ZONE;
