-- Flyway V11: TourAPI 장소 부가 정보
-- content_type_id      : TourAPI 장소 유형(12 관광지, 14 문화시설, 15 행사, 25 여행코스, 28 레포츠, 32 숙박, 38 쇼핑, 39 음식점)
-- thumbnail_url        : 목록용 작은 대표 이미지(firstimage2)
-- image_copyright_type : 대표 이미지 공공누리 유형(cpyrhtDivCd, 예: Type1=출처표시, Type3=출처표시+변경금지)
-- detail_info          : 상세 부가 정보(홈페이지/주차/문의처/유모차/이용요금) - 상세 화면 첫 조회 시 채움
-- gallery_images       : 상세 사진 목록(detailImage2: 원본/작은 이미지 URL, 공공누리 유형) - 상세 화면 첫 조회 시 채움
ALTER TABLE places ADD COLUMN content_type_id VARCHAR(10);
ALTER TABLE places ADD COLUMN thumbnail_url TEXT;
ALTER TABLE places ADD COLUMN image_copyright_type VARCHAR(10);
ALTER TABLE places ADD COLUMN detail_info JSONB;
ALTER TABLE places ADD COLUMN gallery_images JSONB;

-- 상세 화면에서 가져오는 항목이 늘어났으므로 기존 지연 적재 기록을 초기화한다(다음 조회 때 새 항목까지 다시 가져온다).
UPDATE places SET detail_synced_at = NULL;
