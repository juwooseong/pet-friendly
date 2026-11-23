package com.petspot.infrastructure.publicdata.batch;

import com.petspot.infrastructure.publicdata.service.TourPetPolicySyncService;
import com.petspot.infrastructure.publicdata.service.TourPlaceImageBackfillService;
import com.petspot.infrastructure.publicdata.service.TourPlaceSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 한국관광공사 TourAPI 반려동물 동반 장소 수집 배치 (TSK-DATA-010 정책 단계, TSK-DATA-012 증분 동기화)
 *
 * <p>한 번 실행하면 두 단계를 차례로 수행한다.
 * <ol>
 *   <li><b>장소 동기화</b> ({@code petTourSyncList2}, {@link TourPlaceSyncService}): 마지막 동기화 날짜부터 오늘까지
 *       날짜별로 <b>바뀐 장소만</b> 받아 등록·갱신·숨김 처리한다(보통 하루 1~2회 호출). 예전에는 매번 전국 목록
 *       ({@code areaBasedList2}, 약 97회)을 다시 받았고 TourAPI가 숨긴 장소도 알 수 없었다.</li>
 *   <li><b>반려동물 정책</b> ({@code detailPetTour2}, {@link TourPetPolicySyncService}): 아직 조회하지 않았거나 장소 정보가
 *       바뀐 장소부터 최대 {@code pet-policy-max-calls}건을 조회·반영한다. 일일 트래픽 한도 안에서 여러 날에 나눠 채운다.</li>
 *   <li><b>사진 보강</b> ({@link TourPlaceImageBackfillService}, TSK-DATA-013): 대표 사진이 없는 장소의 상세를 최대
 *       {@code image-backfill-max-places}곳 미리 받아, 상세 사진 첫 장을 대표 사진으로 쓴다.</li>
 * </ol>
 *
 * <p>실행 방법: (1) {@code public-data.batch.enabled=true}이면 {@code cron} 일정에 따라 실행, (2) 지금 한 번만 실행하려면
 * {@link TourDataBatchRunner}({@code public-data.batch.run-on-startup=true}) 사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TourDataBatch {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final TourPlaceSyncService tourPlaceSyncService;
    private final TourPetPolicySyncService tourPetPolicySyncService;
    private final TourPlaceImageBackfillService tourPlaceImageBackfillService;

    /** 한 번 실행에서 상세를 미리 받아 사진을 채울 최대 장소 수 (장소당 상세 API 2~3회, TSK-DATA-013) */
    @Value("${public-data.batch.image-backfill-max-places:100}")
    private int imageBackfillMaxPlaces = 100;

    @Value("${public-data.batch.enabled:false}")
    private boolean batchEnabled = false; // 설정 누락 시에도 자동 수집이 돌지 않도록 안전한 기본값(비활성)

    /** 장소 동기화 단계 실행 여부 (false면 정책 조회만 이어서 진행) */
    @Value("${public-data.batch.sync-place-list:true}")
    private boolean syncPlaceList = true;

    /**
     * 동기화 진행 날짜가 아직 저장되지 않았을 때(첫 실행) 이 날짜부터 바뀐 장소를 받는다(yyyyMMdd, 예: 최초 전국 적재일).
     * 비워 두면 첫 실행은 전체(숨긴 장소 포함 약 101회 호출)를 받는다.
     */
    @Value("${public-data.batch.sync-since:}")
    private String syncSince = "";

    /** 한 번 실행에서 정책 조회(detailPetTour2)에 쓸 최대 호출 수 */
    @Value("${public-data.batch.pet-policy-max-calls:800}")
    private int petPolicyMaxCalls = 800;

    /** 정책 조회 호출 간 대기(ms) */
    @Value("${public-data.batch.pet-policy-call-delay-ms:100}")
    private long petPolicyCallDelayMs = 100;

    /**
     * 예약 실행. {@code public-data.batch.enabled=false}(기본값)이면 아무것도 하지 않는다.
     */
    @Scheduled(cron = "${public-data.batch.cron:0 0 3 * * *}", zone = "Asia/Seoul")
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
        log.info("[BATCH START] TourDataBatch (place sync: {}, pet policy max calls: {})", syncPlaceList, petPolicyMaxCalls);
        log.info("==================================================");

        TourPlaceSyncService.SyncResult placeResult = syncPlaceList
                ? tourPlaceSyncService.syncChanges(LocalDate.now(KOREA), parseSyncSince())
                : TourPlaceSyncService.SyncResult.notRun();
        TourPetPolicySyncService.SyncResult policyResult = placeResult.quotaExceeded()
                ? new TourPetPolicySyncService.SyncResult(0, 0, 0, 0, "건너뜀: 장소 동기화에서 일일 호출 한도 초과")
                : tourPetPolicySyncService.syncPendingPolicies(petPolicyMaxCalls, petPolicyCallDelayMs);
        // 사진 보강은 상세 API(detailCommon2/detailIntro2/detailImage2)를 써서 정책 조회(detailPetTour2) 한도와 별개다
        TourPlaceImageBackfillService.BackfillResult imageResult = placeResult.quotaExceeded()
                ? new TourPlaceImageBackfillService.BackfillResult(0, 0, 0, 0, "건너뜀: 장소 동기화에서 일일 호출 한도 초과")
                : tourPlaceImageBackfillService.backfill(imageBackfillMaxPlaces, petPolicyCallDelayMs);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("==================================================");
        log.info("[BATCH END] place sync: {}", placeResult);
        log.info("[BATCH END] pet policy: {}", policyResult);
        log.info("[BATCH END] image backfill: {}", imageResult);
        log.info("[BATCH END] elapsed={} ms", elapsed);
        log.info("==================================================");
        return new BatchResult(placeResult, policyResult, imageResult);
    }

    private LocalDate parseSyncSince() {
        if (syncSince == null || syncSince.isBlank()) {
            return null;
        }
        return LocalDate.parse(syncSince.trim(), DateTimeFormatter.BASIC_ISO_DATE);
    }

    public record BatchResult(TourPlaceSyncService.SyncResult placeSync, TourPetPolicySyncService.SyncResult petPolicy,
                              TourPlaceImageBackfillService.BackfillResult imageBackfill) {
    }
}
