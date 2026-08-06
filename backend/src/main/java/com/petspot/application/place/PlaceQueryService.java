package com.petspot.application.place;

import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import com.petspot.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
     * 반경 거리 및 복합 조건을 만족하는 장소 목록 검색
     *
     * @param condition 검색 조건
     * @return 장소 검색 DTO 리스트
     */
    public List<PlaceSearchResponseDto> searchPlaces(PlaceSearchCondition condition) {
        log.info("[PLACE SEARCH REQUEST] Service query initiated with condition: lat={}, lon={}, radiusKm={}, keyword={}, category={}",
                condition.getLatitude(), condition.getLongitude(), condition.getRadiusKm(),
                condition.getKeyword(), condition.getCategory());

        long startTime = System.currentTimeMillis();
        List<PlaceSearchResponseDto> places = placeRepository.searchPlaces(condition);
        long elapsedTime = System.currentTimeMillis() - startTime;

        log.info("[PLACE SEARCH RESPONSE] Service search completed. Results count: {}, Execution time: {} ms",
                places.size(), elapsedTime);

        return places;
    }
}
