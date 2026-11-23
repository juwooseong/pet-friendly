-- TSK-DATA-013: 장소 사진 정리

-- 1. 관광공사 사진 서버의 http 주소를 https로 (HTTPS 사이트에서 막히지 않도록, 같은 경로가 https로 열림을 확인)
UPDATE places SET image_url = regexp_replace(image_url, '^http://tong\.visitkorea\.or\.kr/', 'https://tong.visitkorea.or.kr/')
WHERE image_url LIKE 'http://tong.visitkorea.or.kr/%';

UPDATE places SET thumbnail_url = regexp_replace(thumbnail_url, '^http://tong\.visitkorea\.or\.kr/', 'https://tong.visitkorea.or.kr/')
WHERE thumbnail_url LIKE 'http://tong.visitkorea.or.kr/%';

UPDATE places SET gallery_images = replace(gallery_images::text, '"http://tong.visitkorea.or.kr/', '"https://tong.visitkorea.or.kr/')::jsonb
WHERE gallery_images::text LIKE '%"http://tong.visitkorea.or.kr/%';

-- 2. 빈 문자열 사진 주소는 "없음"(NULL)으로
UPDATE places SET image_url = NULL WHERE image_url = '';
UPDATE places SET thumbnail_url = NULL WHERE thumbnail_url = '';

-- 3. 대표 사진이 없는데 상세 사진을 이미 받아 둔 장소는 첫 장을 대표 사진으로
UPDATE places
SET image_url = gallery_images -> 0 ->> 'url',
    thumbnail_url = gallery_images -> 0 ->> 'thumbnailUrl',
    image_copyright_type = gallery_images -> 0 ->> 'copyrightType'
WHERE image_url IS NULL
  AND jsonb_typeof(gallery_images) = 'array'
  AND jsonb_array_length(gallery_images) > 0;
