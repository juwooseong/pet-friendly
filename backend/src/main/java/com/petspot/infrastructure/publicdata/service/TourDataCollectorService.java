package com.petspot.infrastructure.publicdata.service;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import com.petspot.infrastructure.publicdata.mapper.TourApiPlaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * TourAPI 데이터 수집, Entity 매핑 및 DB 저장 파이프라인 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourDataCollectorService {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int FIRST_PAGE = 1;

    private final TourApiClient tourApiClient;
    private final TourApiPlaceMapper tourApiPlaceMapper;
    private final PlaceRepository placeRepository;

    /**
     * 지정된 페이지 수집 및 DTO 리스트 반환
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
     * 전체 공공데이터를 순회 수집
     */
    public List<TourApiPlaceItemDto> collectAllPlaces(int pageSize) {
        int actualPageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        List<TourApiPlaceItemDto> allCollectedItems = new ArrayList<>();

        log.info("Starting full TourAPI collection with pageSize={}", actualPageSize);

        TourApiResponseDto firstPageResponse = tourApiClient.fetchPetPlaces(actualPageSize, FIRST_PAGE);
        if (firstPageResponse == null || firstPageResponse.getResponse() == null || firstPageResponse.getResponse().getBody() == null) {
            log.warn("First page response is null or empty. Collection aborted.");
            return Collections.emptyList();
        }

        TourApiResponseDto.Body body = firstPageResponse.getResponse().getBody();
        int totalCount = body.getTotalCount() != null ? body.getTotalCount() : 0;
        allCollectedItems.addAll(body.getItemList());

        int totalPages = (int) Math.ceil((double) totalCount / actualPageSize);
        log.info("Total items to collect: {}, Total pages: {}", totalCount, totalPages);

        for (int pageNo = 2; pageNo <= totalPages; pageNo++) {
            List<TourApiPlaceItemDto> pageItems = collectPage(actualPageSize, pageNo);
            allCollectedItems.addAll(pageItems);
        }

        log.info("Full TourAPI collection completed. Total items collected: {}", allCollectedItems.size());
        return allCollectedItems;
    }

    /**
     * 수집한 TourApiPlaceItemDto 리스트를 Entity로 변환하고 중복 방지 저장 (Transactional)
     *
     * @param items DTO 리스트
     * @return [신규 저장 건수, 중복/업데이트 건수]
     */
    @Transactional
    public int[] saveCollectedPlaces(List<TourApiPlaceItemDto> items) {
        if (items == null || items.isEmpty()) {
            log.info("No place items to save.");
            return new int[]{0, 0};
        }

        long startTime = System.currentTimeMillis();
        log.info("Starting place entity persistence for {} items...", items.size());

        int savedCount = 0;
        int duplicateCount = 0;

        for (TourApiPlaceItemDto dto : items) {
            if (dto.getContentId() == null || dto.getContentId().isBlank()) {
                continue;
            }

            Optional<Place> existingPlaceOpt = placeRepository.findByPublicDataId(dto.getContentId());

            if (existingPlaceOpt.isPresent()) {
                // 중복 데이터 존재하는 경우: 기존 Entity 정보 업데이트
                Place existingPlace = existingPlaceOpt.get();
                Place tempEntity = tourApiPlaceMapper.toEntity(dto);
                existingPlace.updateDetails(
                        tempEntity.getName(),
                        tempEntity.getAddress(),
                        tempEntity.getLocation(),
                        tempEntity.getLatitude(),
                        tempEntity.getLongitude(),
                        tempEntity.getImageUrl(),
                        tempEntity.getPhone(),
                        tempEntity.getCategoryName()
                );
                duplicateCount++;
            } else {
                // 신규 데이터인 경우: Entity 변환 후 DB 저장
                Place newPlace = tourApiPlaceMapper.toEntity(dto);
                placeRepository.save(newPlace);
                savedCount++;
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[PERSISTENCE SUMMARY] Total Items: {}, Saved: {}, Duplicates Updated: {}, Elapsed Time: {} ms",
                items.size(), savedCount, duplicateCount, elapsedTime);

        return new int[]{savedCount, duplicateCount};
    }
}
