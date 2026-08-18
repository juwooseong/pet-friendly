package com.petspot.application.place;

import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.global.dto.PageResponse;
import com.petspot.global.error.exception.PlaceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 장소 검색 애플리케이션 서비스 (오케스트레이션 수행)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceQueryService {

    private final PlaceRepository placeRepository;

    /**
     * 반경 거리 및 복합 조건을 만족하는 장소 목록 검색 (기존 리스트 지원)
     */
    public List<PlaceSearchResponseDto> searchPlaces(PlaceSearchCondition condition) {
        return placeRepository.searchPlaces(condition);
    }

    /**
     * 반경 거리 및 복합 조건을 만족하는 장소 목록 검색 (Spring Pageable 페이징 지원)
     *
     * @param condition 검색 조건
     * @param pageable  페이징 정보
     * @return PageResponse 장소 검색 결과 래퍼
     */
    public PageResponse<PlaceSearchResponseDto> searchPlaces(PlaceSearchCondition condition, Pageable pageable) {
        log.info("[PLACE SEARCH REQUEST] Service query with Pageable: lat={}, lon={}, radiusKm={}, page={}, size={}",
                condition.getLatitude(), condition.getLongitude(), condition.getRadiusKm(),
                pageable.getPageNumber(), pageable.getPageSize());

        long startTime = System.currentTimeMillis();
        Page<PlaceSearchResponseDto> pageResult = placeRepository.searchPlaces(condition, pageable);
        long elapsedTime = System.currentTimeMillis() - startTime;

        log.info("[PLACE SEARCH RESPONSE] Search completed. Page: {}, Size: {}, Total: {}, Execution time: {} ms",
                pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(), elapsedTime);

        return PageResponse.from(pageResult);
    }

    /**
     * 장소 ID로 상세 정보 단건 조회
     *
     * @param placeId 조회할 장소 ID
     * @return 장소 상세 정보 (검색 결과와 동일한 Projection DTO 재사용)
     * @throws PlaceNotFoundException 존재하지 않는 장소 ID인 경우
     */
    public PlaceSearchResponseDto getPlaceDetail(UUID placeId) {
        log.info("[PLACE DETAIL REQUEST] placeId={}", placeId);
        return placeRepository.findPlaceDetail(placeId)
                .orElseThrow(() -> new PlaceNotFoundException("장소 정보를 찾을 수 없습니다."));
    }
}
