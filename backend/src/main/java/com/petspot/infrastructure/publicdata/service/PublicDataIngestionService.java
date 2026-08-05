package com.petspot.infrastructure.publicdata.service;

import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 공공데이터 TourAPI 파싱 및 조회를 담당하는 전용 서비스
 * (다음 Task인 TSK-BE-005 Batch Ingestion Pipeline 및 데이터 매핑에서 주입받아 사용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDataIngestionService {

    private final TourApiClient tourApiClient;

    /**
     * 지정된 페이지의 펫 동반 장소 목록 수집
     *
     * @param numOfRows 가져올 개수
     * @param pageNo   페이지 번호
     * @return TourApiPlaceItemDto 리스트
     */
    public List<TourApiPlaceItemDto> fetchPetPlaces(int numOfRows, int pageNo) {
        return fetchPetPlaces(numOfRows, pageNo, null);
    }

    /**
     * 카테고리별 지정된 페이지의 펫 동반 장소 목록 수집
     *
     * @param numOfRows    가져올 개수
     * @param pageNo      페이지 번호
     * @param contentTypeId 콘텐츠 타입 ID
     * @return TourApiPlaceItemDto 리스트
     */
    public List<TourApiPlaceItemDto> fetchPetPlaces(int numOfRows, int pageNo, String contentTypeId) {
        log.info("Fetching public pet places from TourAPI: pageNo={}, numOfRows={}, contentTypeId={}",
                pageNo, numOfRows, contentTypeId);

        try {
            TourApiResponseDto responseDto = tourApiClient.fetchPetPlaces(numOfRows, pageNo, contentTypeId);

            if (responseDto.getResponse() == null || responseDto.getResponse().getBody() == null) {
                log.warn("Empty body response from TourAPI: pageNo={}", pageNo);
                return Collections.emptyList();
            }

            List<TourApiPlaceItemDto> items = responseDto.getResponse().getBody().getItemList();
            log.info("Successfully fetched {} pet place items from TourAPI (totalCount={})",
                    items.size(), responseDto.getResponse().getBody().getTotalCount());

            return items;

        } catch (PublicDataException e) {
            log.error("Failed to fetch public pet places from TourAPI: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 수집 가능한 총 데이터 건수 조회
     */
    public int fetchTotalCount() {
        TourApiResponseDto responseDto = tourApiClient.fetchPetPlaces(1, 1);
        if (responseDto.getResponse() != null && responseDto.getResponse().getBody() != null) {
            Integer totalCount = responseDto.getResponse().getBody().getTotalCount();
            return totalCount != null ? totalCount : 0;
        }
        return 0;
    }
}
