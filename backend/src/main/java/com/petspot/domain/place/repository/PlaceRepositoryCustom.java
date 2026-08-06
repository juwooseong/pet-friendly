package com.petspot.domain.place.repository;

import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * QueryDSL + PostGIS 공간 검색 커스텀 인터페이스
 */
public interface PlaceRepositoryCustom {

    /**
     * 사용자 중심 위치 기준 반경 거리 N km 내 펫 동반 장소 다중 필터 검색 (리스트)
     */
    List<PlaceSearchResponseDto> searchPlaces(PlaceSearchCondition condition);

    /**
     * 사용자 중심 위치 기준 반경 거리 N km 내 펫 동반 장소 다중 필터 검색 (Spring Pageable 페이징)
     */
    Page<PlaceSearchResponseDto> searchPlaces(PlaceSearchCondition condition, Pageable pageable);
}
