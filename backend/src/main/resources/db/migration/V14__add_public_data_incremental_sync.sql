-- TSK-DATA-012: TourAPI petTourSyncList2 기반 증분 동기화

-- TourAPI가 숨긴(showflag=0, 서비스 중단·중복 정리 등) 장소. 검색·지도·홈에서 뺀다.
-- 리뷰·즐겨찾기가 연결돼 있을 수 있어 지우지 않고, 다시 노출되면(showflag=1) 비운다.
ALTER TABLE places ADD COLUMN hidden_at TIMESTAMPTZ;

-- TourAPI가 알려 준 장소 정보 수정 시각(modifiedtime). 이 값이 바뀐 장소만 정책·상세를 다시 조회한다.
ALTER TABLE places ADD COLUMN public_data_modified_at TIMESTAMPTZ;

-- 동기화 진행 상태. 마지막으로 빠짐없이 반영한 날짜를 저장해 다음 실행이 그 날부터 이어 간다.
CREATE TABLE public_data_sync_state (
    name VARCHAR(50) PRIMARY KEY,
    last_synced_date DATE NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
