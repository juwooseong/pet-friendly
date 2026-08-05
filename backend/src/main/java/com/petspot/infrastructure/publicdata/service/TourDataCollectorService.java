package com.petspot.infrastructure.publicdata.service;

import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TourAPI 데이터 수집 및 정제 오케스트레이션 서비스
 * (TourApiClient 호출 후 DTO 수신 및 다음 Task에서 Entity 변환/저장으로 넘겨줄 프로세서)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourDataCollectorService {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int FIRST_PAGE = 1;

    private final TourApiClient tourApiClient;

    /**
     * 지정된 페이지 크기 및 페이지 번호로 단일 페이지 수집
     *
     * @param pageSize 페이지당 항목 수
     * @param pageNo   페이지 번호
     * @return 수집된 TourApiPlaceItemDto 리스트
     */
    public List<TourApiPlaceItemDto> collectPage(int pageSize, int pageNo) {
        log.debug("Collecting TourAPI page: pageNo={}, pageSize={}", pageNo, pageSize);
        try {
            TourApiResponseDto responseDto = tourApiClient.fetchPetPlaces(pageSize, pageNo);
            if (responseDto == null || responseDto.getResponse() == null || responseDto.getResponse().getBody() == null) {
                log.warn("Empty response body received for pageNo={}", pageNo);
                return Collections.emptyList();
            }
            return responseDto.getResponse().getBody().getItemList();
        } catch (PublicDataException e) {
            log.error("Failed to collect TourAPI page {}: {}", pageNo, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during TourAPI collection on page {}: {}", pageNo, e.getMessage(), e);
            throw new PublicDataException("TourAPI 데이터 수집 중 예외 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 전체 반려동물 동반 장소 데이터를 순회 수집
     *
     * @param pageSize 페이지당 수집 건수
     * @return 전체 수집된 TourApiPlaceItemDto 리스트
     */
    public List<TourApiPlaceItemDto> collectAllPlaces(int pageSize) {
        int actualPageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        List<TourApiPlaceItemDto> allCollectedItems = new ArrayList<>();

        log.info("Starting full TourAPI collection with pageSize={}", actualPageSize);

        // 1. 첫 페이지 호출하여 전체 데이터 건수(totalCount) 파악
        TourApiResponseDto firstPageResponse = tourApiClient.fetchPetPlaces(actualPageSize, FIRST_PAGE);
        if (firstPageResponse == null || firstPageResponse.getResponse() == null || firstPageResponse.getResponse().getBody() == null) {
            log.warn("First page response is null or empty. Collection aborted.");
            return Collections.emptyList();
        }

        TourApiResponseDto.Body body = firstPageResponse.getResponse().getBody();
        int totalCount = body.getTotalCount() != null ? body.getTotalCount() : 0;
        List<TourApiPlaceItemDto> firstPageItems = body.getItemList();
        allCollectedItems.addAll(firstPageItems);

        int totalPages = (int) Math.ceil((double) totalCount / actualPageSize);
        log.info("Total items to collect: {}, Total pages: {}", totalCount, totalPages);

        // 2. 2페이지부터 나머지 수집
        for (int pageNo = 2; pageNo <= totalPages; pageNo++) {
            List<TourApiPlaceItemDto> pageItems = collectPage(actualPageSize, pageNo);
            allCollectedItems.addAll(pageItems);
        }

        log.info("Full TourAPI collection completed. Total items collected: {}", allCollectedItems.size());
        return allCollectedItems;
    }
}
