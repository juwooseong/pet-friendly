package com.petspot.infrastructure.publicdata.batch;

import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import com.petspot.infrastructure.publicdata.service.TourDataCollectorService;
import com.petspot.infrastructure.publicdata.service.TourPetPolicySyncService;
import com.petspot.infrastructure.publicdata.service.TourPetPolicySyncService.SyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 한국관광공사 TourAPI 반려동물 동반 장소 수집 배치 (TSK-DATA-010에서 정책 조회 단계 추가)
 *
 * <p>한 번 실행하면 두 단계를 차례로 수행한다.
 * <ol>
 *   <li><b>장소 목록</b> ({@code areaBasedList2}): 전국 목록을 페이지 단위로 받아 <b>페이지마다 바로 저장</b>한다.
 *       예전에는 전체를 메모리에 모은 뒤 마지막에 한꺼번에 저장해, 중간 페이지 하나만 실패해도 아무것도 저장되지 않았다.</li>
 *   <li><b>반려동물 정책</b> ({@code detailPetTour2}): 아직 정책을 조회하지 않은 장소부터 최대
 *       {@code pet-policy-max-calls}건을 조회·해석해 반영한다({@link TourPetPolicySyncService}). 장소마다 1회 호출이 필요해
 *       일일 트래픽 한도 안에서 여러 날에 나눠 채운다.</li>
 * </ol>
 *
 * <p>실행 방법: (1) {@code public-data.batch.enabled=true}이면 {@code cron} 일정에 따라 실행, (2) 지금 한 번만 실행하려면
 * {@link TourDataBatchRunner}({@code public-data.batch.run-on-startup=true}) 사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TourDataBatch {

    private static final int DEFAULT_PAGE_SIZE = 100;
    /** 페이지 수 안전 상한 (전국 약 9,600건 / 100 = 97페이지) */
    private static final int MAX_PAGES = 500;
    private static final int MAX_CONSECUTIVE_PAGE_FAILURES = 3;
    private static final String QUOTA_EXCEEDED_MARKER = "LIMITED_NUMBER_OF_SERVICE_REQUESTS";

    private final TourDataCollectorService tourDataCollectorService;
    private final TourPetPolicySyncService tourPetPolicySyncService;

    @Value("${public-data.batch.enabled:false}")
    private boolean batchEnabled = false; // 설정 누락 시에도 자동 수집이 돌지 않도록 안전한 기본값(비활성)

    @Value("${public-data.batch.page-size:100}")
    private int pageSize = 100;

    /** 장소 목록 단계 실행 여부 (false면 정책 조회만 이어서 진행 - 목록 호출 약 97회 절약) */
    @Value("${public-data.batch.sync-place-list:true}")
    private boolean syncPlaceList = true;

    /** 한 번 실행에서 정책 조회(detailPetTour2)에 쓸 최대 호출 수. 목록 단계 호출(약 97회)과 합쳐 일일 한도 안으로 */
    @Value("${public-data.batch.pet-policy-max-calls:800}")
    private int petPolicyMaxCalls = 800;

    /** 정책 조회 호출 간 대기(ms) */
    @Value("${public-data.batch.pet-policy-call-delay-ms:100}")
    private long petPolicyCallDelayMs = 100;

    /**
     * 예약 실행. {@code public-data.batch.enabled=false}(기본값)이면 아무것도 하지 않는다.
     */
    @Scheduled(cron = "${public-data.batch.cron:0 0 3 * * *}")
    public void runIngestionBatch() {
        if (!batchEnabled) {
            log.info("TourDataBatch is disabled in configuration. Skipping execution.");
            return;
        }
        runOnce();
    }

    /**
     * 설정의 enabled 여부와 관계없이 지금 한 번 실행한다(명시적 수동 실행용).
     */
    public BatchResult runOnce() {
        long startTime = System.currentTimeMillis();
        log.info("==================================================");
        log.info("[BATCH START] TourDataBatch (place list: {}, pet policy max calls: {})", syncPlaceList, petPolicyMaxCalls);
        log.info("==================================================");

        PlaceListResult listResult = syncPlaceList ? ingestPlaceList() : PlaceListResult.skipped();
        SyncResult policyResult = listResult.quotaExceeded()
                ? new SyncResult(0, 0, 0, 0, "건너뜀: 목록 단계에서 일일 호출 한도 초과")
                : tourPetPolicySyncService.syncPendingPolicies(petPolicyMaxCalls, petPolicyCallDelayMs);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("==================================================");
        log.info("[BATCH END] place list: pages={}, saved={}, updated={}, failedPages={}, stop={}",
                listResult.pages(), listResult.saved(), listResult.updated(), listResult.failedPages(), listResult.stopReason());
        log.info("[BATCH END] pet policy: {}", policyResult);
        log.info("[BATCH END] elapsed={} ms", elapsed);
        log.info("==================================================");
        return new BatchResult(listResult, policyResult);
    }

    /** 장소 목록을 페이지 단위로 받아 페이지마다 저장한다. 실패한 페이지는 건너뛰고 다음 페이지로 진행한다. */
    private PlaceListResult ingestPlaceList() {
        int actualPageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        int pages = 0;
        int saved = 0;
        int updated = 0;
        int failedPages = 0;
        int consecutiveFailures = 0;
        String stopReason = null;
        boolean quotaExceeded = false;

        for (int pageNo = 1; pageNo <= MAX_PAGES; pageNo++) {
            List<TourApiPlaceItemDto> items;
            try {
                items = tourDataCollectorService.collectPage(actualPageSize, pageNo);
                consecutiveFailures = 0;
            } catch (PublicDataException e) {
                failedPages++;
                consecutiveFailures++;
                log.error("[BATCH] place list page {} failed: {}", pageNo, e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains(QUOTA_EXCEEDED_MARKER)) {
                    quotaExceeded = true;
                    stopReason = "일일 호출 한도 초과";
                    break;
                }
                if (pageNo == 1 || consecutiveFailures >= MAX_CONSECUTIVE_PAGE_FAILURES) {
                    // 첫 페이지 실패 = API 자체 문제일 가능성이 높아 계속 호출해도 의미가 없다
                    stopReason = "중단: " + e.getMessage();
                    break;
                }
                continue;
            }

            if (items.isEmpty()) {
                break;
            }
            pages++;
            int[] result = tourDataCollectorService.saveCollectedPlaces(items); // 페이지마다 별도 트랜잭션
            saved += result[0];
            updated += result[1];

            if (items.size() < actualPageSize) {
                break; // 마지막 페이지
            }
        }
        return new PlaceListResult(pages, saved, updated, failedPages, stopReason, quotaExceeded);
    }

    public record PlaceListResult(int pages, int saved, int updated, int failedPages, String stopReason,
                                  boolean quotaExceeded) {
        static PlaceListResult skipped() {
            return new PlaceListResult(0, 0, 0, 0, "건너뜀(sync-place-list=false)", false);
        }
    }

    public record BatchResult(PlaceListResult placeList, SyncResult petPolicy) {
    }
}
