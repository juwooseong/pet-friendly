package com.petspot.infrastructure.publicdata.service;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 대표 사진이 없는 장소의 TourAPI 상세를 미리 받아 사진을 채운다 (TSK-DATA-013).
 *
 * <p>목록 응답(areaBasedList2·petTourSyncList2)에 대표 사진이 없는 장소가 있다(숙박 78%, 음식점·카페 44%). 이런 장소도
 * 상세 사진(detailImage2)은 있는 경우가 있어, 상세 화면을 열기 전에 배치가 미리 받아 첫 사진을 대표 사진으로 쓴다
 * ({@link Place#applyTourApiDetail}). 장소당 상세 API 2~3회 호출이라 한 번에 {@code maxPlaces}곳까지만 처리하고,
 * 상세를 받은 장소는 다시 고르지 않는다(사진이 끝내 없어도). 소개·운영시간도 함께 채워진다.
 */
@Slf4j
@Service
public class TourPlaceImageBackfillService {

    private static final List<String> FATAL_ERROR_MARKERS = List.of(
            "LIMITED_NUMBER_OF_SERVICE_REQUESTS", "SERVICE_KEY_IS_NOT_REGISTERED", "SERVICE_ACCESS_DENIED");
    static final int MAX_CONSECUTIVE_FAILURES = 5;

    private final PlaceRepository placeRepository;
    private final PlaceDetailEnrichmentService enrichmentService;

    public TourPlaceImageBackfillService(PlaceRepository placeRepository, PlaceDetailEnrichmentService enrichmentService) {
        this.placeRepository = placeRepository;
        this.enrichmentService = enrichmentService;
    }

    public BackfillResult backfill(int maxPlaces, long delayMillis) {
        if (maxPlaces <= 0) {
            return new BackfillResult(0, 0, 0, 0, null);
        }
        List<UUID> targets = placeRepository.findMissingImagePendingDetail(PageRequest.of(0, maxPlaces))
                .stream().map(Place::getId).toList();
        log.info("[IMAGE BACKFILL] start: targets={}", targets.size());

        int enriched = 0;
        int withImage = 0;
        int failed = 0;
        int consecutiveFailures = 0;
        String stopReason = null;
        for (UUID placeId : targets) {
            PlaceDetailEnrichmentService.EnrichResult result = enrichmentService.enrich(placeId);
            if (result.failureReason() != null) {
                failed++;
                consecutiveFailures++;
                if (FATAL_ERROR_MARKERS.stream().anyMatch(result.failureReason()::contains)) {
                    stopReason = "중단: " + result.failureReason();
                    break;
                }
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    stopReason = "중단: 연속 " + consecutiveFailures + "회 실패";
                    break;
                }
                continue;
            }
            consecutiveFailures = 0;
            if (result.saved()) {
                enriched++;
                if (result.hasImages()) {
                    withImage++;
                }
            }
            sleep(delayMillis);
        }

        BackfillResult result = new BackfillResult(targets.size(), enriched, withImage, failed, stopReason);
        log.info("[IMAGE BACKFILL] done: {}", result);
        return result;
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @param targeted   이번 실행 대상
     * @param enriched   상세를 받아 저장한 장소
     * @param withImage  그중 사진이 있어 대표 사진이 생긴 장소
     * @param failed     조회 실패(다음 실행에서 재시도)
     * @param stopReason 중간에 멈췄으면 사유
     */
    public record BackfillResult(int targeted, int enriched, int withImage, int failed, String stopReason) {
    }
}
