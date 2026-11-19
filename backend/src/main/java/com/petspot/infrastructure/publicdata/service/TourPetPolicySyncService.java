package com.petspot.infrastructure.publicdata.service;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiPetTourItemDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import com.petspot.infrastructure.publicdata.mapper.ParsedPetPolicy;
import com.petspot.infrastructure.publicdata.mapper.TourApiPetPolicyParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 공공데이터 장소의 반려동물 정책을 TourAPI {@code detailPetTour2}로 채운다 (TSK-DATA-010, ADR-009).
 *
 * <p>{@code detailPetTour2}는 장소마다 1회 호출해야 하므로(목록 조회 불가) 전국 9천여 곳을 한 번에 처리할 수 없다.
 * 한 번 실행에서 {@code maxCalls}건까지만 호출하고, 아직 조회하지 않은 장소({@code petPolicySyncedAt == null})부터
 * 처리하므로 여러 날에 나눠 실행해도 이어서 진행된다.
 *
 * <p>장소 1건마다 별도 트랜잭션으로 저장해, 중간에 멈춰도 이미 처리한 장소는 보존된다.
 * 외부 API 호출은 트랜잭션 밖에서 한다(DB 커넥션을 붙잡고 네트워크를 기다리지 않도록).
 *
 * <p>일일 호출 한도 초과·서비스키 오류처럼 이후 호출도 모두 실패할 오류이거나, 연속 실패가
 * {@value #MAX_CONSECUTIVE_FAILURES}회 이어지면 즉시 멈춘다(의미 없는 호출 낭비 방지).
 */
@Slf4j
@Service
public class TourPetPolicySyncService {

    /** data.go.kr 일일 트래픽 초과(reasonCode 22) / 서비스키 문제 - 이후 호출도 전부 실패하므로 중단 */
    private static final List<String> FATAL_ERROR_MARKERS = List.of(
            "LIMITED_NUMBER_OF_SERVICE_REQUESTS", "SERVICE_KEY_IS_NOT_REGISTERED", "SERVICE_ACCESS_DENIED");
    static final int MAX_CONSECUTIVE_FAILURES = 5;

    private final TourApiClient tourApiClient;
    private final TourApiPetPolicyParser parser;
    private final PlaceRepository placeRepository;
    private final TransactionTemplate transactionTemplate;

    public TourPetPolicySyncService(TourApiClient tourApiClient, TourApiPetPolicyParser parser,
                                    PlaceRepository placeRepository, PlatformTransactionManager transactionManager) {
        this.tourApiClient = tourApiClient;
        this.parser = parser;
        this.placeRepository = placeRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 정책 미조회 장소를 최대 {@code maxCalls}건 처리한다.
     *
     * @param maxCalls    이번 실행에서 사용할 최대 API 호출 수 (일일 한도 안에서 지정)
     * @param delayMillis 호출 간 대기(ms). 공공 API 서버 부하를 줄이기 위함
     */
    public SyncResult syncPendingPolicies(int maxCalls, long delayMillis) {
        if (maxCalls <= 0) {
            return new SyncResult(0, 0, 0, 0, null);
        }
        // 엔티티 전체가 아닌 식별자만 사용한다(트랜잭션 밖이라 준영속 상태)
        List<Place> pending = placeRepository
                .findByPublicDataIdIsNotNullAndPetPolicySyncedAtIsNullOrderByCreatedAtDesc(PageRequest.of(0, maxCalls));
        log.info("[PET POLICY SYNC] start: pending(this run)={}, maxCalls={}", pending.size(), maxCalls);

        int applied = 0;
        int notProvided = 0;
        int failed = 0;
        int consecutiveFailures = 0;
        String stopReason = null;

        for (Place place : pending) {
            UUID placeId = place.getId();
            String contentId = place.getPublicDataId();

            Optional<TourApiPetTourItemDto> detail;
            try {
                detail = tourApiClient.fetchPetTourDetail(contentId);
                consecutiveFailures = 0;
            } catch (PublicDataException e) {
                failed++;
                consecutiveFailures++;
                log.warn("[PET POLICY SYNC] fetch failed: contentId={}, reason={}", contentId, e.getMessage());
                if (isFatal(e)) {
                    stopReason = "중단: " + e.getMessage();
                    break;
                }
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    stopReason = "중단: 연속 " + consecutiveFailures + "회 실패";
                    break;
                }
                continue; // 조회 시각을 남기지 않으므로 다음 실행에서 다시 시도된다
            }

            boolean hasPolicy = detail.isPresent();
            transactionTemplate.executeWithoutResult(status -> apply(placeId, detail.orElse(null)));
            if (hasPolicy) {
                applied++;
            } else {
                notProvided++;
            }
            sleep(delayMillis);
        }

        SyncResult result = new SyncResult(pending.size(), applied, notProvided, failed, stopReason);
        log.info("[PET POLICY SYNC] done: {}", result);
        return result;
    }

    private void apply(UUID placeId, TourApiPetTourItemDto detail) {
        Place place = placeRepository.findById(placeId).orElse(null);
        if (place == null) {
            return; // 처리 중 삭제된 장소
        }
        ZonedDateTime now = ZonedDateTime.now();
        if (detail == null) {
            place.markTourApiPetPolicyNotProvided(now);
            return;
        }
        ParsedPetPolicy policy = parser.parse(detail);
        place.applyTourApiPetPolicy(policy.maxWeightLimitKg(), policy.allowedSizes(), policy.indoorAllowed(),
                toDetailMap(policy, detail), now);
    }

    /** Place 전용 컬럼이 없는 항목과 원문. null(미확인) 항목은 키를 만들지 않는다. */
    static Map<String, Object> toDetailMap(ParsedPetPolicy policy, TourApiPetTourItemDto detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfPresent(map, "accompanyScope", policy.accompanyScope() != null ? policy.accompanyScope().name() : null);
        putIfPresent(map, "maxPetCount", policy.maxPetCount());
        putIfPresent(map, "leashRequired", policy.leashRequired());
        putIfPresent(map, "carrierRequired", policy.carrierRequired());
        putIfPresent(map, "vaccinationRequired", policy.vaccinationRequired());
        putIfPresent(map, "muzzleRequiredForDangerousDogs", policy.muzzleRequiredForDangerousDogs());
        putIfPresent(map, "allowedPetsText", policy.allowedPetsText());
        putIfPresent(map, "requirementsText", policy.requirementsText());
        putIfPresent(map, "etcText", policy.etcText());
        putIfPresent(map, "facilities", blankToNull(detail.getFacilities()));
        putIfPresent(map, "furnishedItems", blankToNull(detail.getFurnishedItems()));
        putIfPresent(map, "purchasableItems", blankToNull(detail.getPurchasableItems()));
        putIfPresent(map, "rentalItems", blankToNull(detail.getRentalItems()));
        return map.isEmpty() ? null : map;
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private boolean isFatal(PublicDataException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        return FATAL_ERROR_MARKERS.stream().anyMatch(msg::contains);
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
     * @param targeted    이번 실행에서 처리 대상으로 고른 장소 수
     * @param applied     정책을 반영한 장소 수
     * @param notProvided TourAPI에 정책이 등록되지 않은 장소 수
     * @param failed      조회 실패 수 (다음 실행에서 재시도)
     * @param stopReason  중간에 멈췄다면 그 사유, 끝까지 처리했으면 null
     */
    public record SyncResult(int targeted, int applied, int notProvided, int failed, String stopReason) {
    }
}
