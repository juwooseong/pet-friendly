-- Flyway V8 (TSK-BE-032): 리뷰가 없는 장소의 평점 표현을 0.0으로 통일
-- 기존: 신규 장소는 DB/엔티티 기본값 5.0으로 생성되고, 리뷰를 전부 삭제하면 0.0으로 재계산되어
--       같은 "리뷰 0건" 상태가 5.0/0.0으로 섞여 있었다. 리뷰 없는 장소가 인기 장소(평점 내림차순) 상위에 노출되는 문제도 있었다.
ALTER TABLE places ALTER COLUMN rating SET DEFAULT 0.0;

UPDATE places
SET rating = 0.0
WHERE review_count IS NULL OR review_count = 0;
