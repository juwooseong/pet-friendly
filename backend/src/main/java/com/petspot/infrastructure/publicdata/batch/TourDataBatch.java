package com.petspot.infrastructure.publicdata.batch;

import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import com.petspot.infrastructure.publicdata.service.TourDataCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 한국관광공사 TourAPI 반려동물 정보 주기적 수집 스케줄러 배치
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TourDataBatch {

    private static final int DEFAULT_PAGE_SIZE = 100;

    private final TourDataCollectorService tourDataCollectorService;

    @Value("${public-data.batch.enabled:true}")
    private boolean batchEnabled = true;

    @Value("${public-data.batch.page-size:100}")
    private int pageSize = 100;

    /**
     * 주기적 공공데이터 수집 배치 스케줄러 메서드
     * application.yml의 public-data.batch.cron 설정을 따른다.
     */
    @Scheduled(cron = "${public-data.batch.cron:0 0 3 * * *}")
    public void runIngestionBatch() {
        if (!batchEnabled) {
            log.info("TourDataBatch is disabled in configuration. Skipping execution.");
            return;
        }

        long startTime = System.currentTimeMillis();
        log.info("==================================================");
        log.info("[BATCH START] TourDataBatch execution started.");
        log.info("==================================================");

        int totalCollectedCount = 0;
        int failureCount = 0;

        try {
            int actualPageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
            List<TourApiPlaceItemDto> collectedItems = tourDataCollectorService.collectAllPlaces(actualPageSize);
            totalCollectedCount = collectedItems.size();

            // 다음 Task(TSK-BE-006)에서 DB 저장/Upsert 및 Entity 변환 확장이 용이하도록 수집 프로세스 후킹 포인트 제공
            processCollectedItems(collectedItems);

        } catch (PublicDataException e) {
            failureCount++;
            log.error("[BATCH ERROR] PublicDataException occurred during TourDataBatch execution: {}", e.getMessage(), e);
        } catch (Exception e) {
            failureCount++;
            log.error("[BATCH ERROR] Unexpected error occurred during TourDataBatch execution: {}", e.getMessage(), e);
            throw new PublicDataException("TourDataBatch 실행 중 치명적 오류 발생: " + e.getMessage(), e);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("==================================================");
            log.info("[BATCH END] TourDataBatch execution finished.");
            log.info("[BATCH SUMMARY] Total Collected Count: {}", totalCollectedCount);
            log.info("[BATCH SUMMARY] Failure Count: {}", failureCount);
            log.info("[BATCH SUMMARY] Total Execution Time: {} ms", elapsedTime);
            log.info("==================================================");
        }
    }

    /**
     * 수집된 데이터를 후처리하는 확장 지점 (다음 Task인 TSK-BE-006 / TSK-BE-007 Entity 마이그레이션 연결용)
     */
    protected void processCollectedItems(List<TourApiPlaceItemDto> items) {
        log.info("Processing {} collected items (Ready for DB Ingestion / Entity Mapping in TSK-BE-006)", items.size());
    }
}
