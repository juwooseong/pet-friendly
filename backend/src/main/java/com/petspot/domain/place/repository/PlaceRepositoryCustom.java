package com.petspot.domain.place.repository;

import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;

import java.util.List;

/**
 * QueryDSL + PostGIS 공간 검색 커스텀 인터페이스
 */
public interface PlaceRepositoryCustom {

    /**
     * 사용자 중심 위치 기준 반경 거리 N km 내 펫 동반 장소 다중 필터 검색
     *
     * @param condition 검색 조건 (위도, 경도, 반경, 키워드, 카테고리 등)
     * @return 거리순 정렬된 PlaceSearchResponseDto 리스트
     */
    List<PlaceSearchResponseDto> searchPlaces(PlaceSearchCondition condition);
}
