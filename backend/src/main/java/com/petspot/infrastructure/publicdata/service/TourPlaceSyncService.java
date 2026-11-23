package com.petspot.infrastructure.publicdata.service;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.entity.PublicDataSyncState;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.domain.place.repository.PublicDataSyncStateRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import com.petspot.infrastructure.publicdata.mapper.TourApiPlaceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * TourAPI {@code petTourSyncList2} 기반 장소 증분 동기화 (TSK-DATA-012).
 *
 * <p>매번 전국 목록(약 100회 호출)을 다시 받지 않고, 마지막 동기화 날짜부터 오늘까지 <b>날짜별로 수정된 장소만</b> 받는다
 * ({@code modifiedtime=yyyyMMdd}, 보통 하루 1~2회 호출). 마지막 날짜도 다시 조회해(하루 겹침) 그날 늦게 바뀐 장소를 놓치지 않는다.
 * <ul>
 *   <li>노출(showflag=1): 없으면 새로 등록, 있으면 기본 정보 갱신. 수정 시각이 새로우면 정책·상세를 다시 조회하도록 표시
 *       (정책은 정책 배치가, 상세는 상세 화면이 다시 가져간다). 숨겨져 있었다면 다시 노출.</li>
 *   <li>숨김(showflag=0): 우리 DB에 있으면 숨김 처리(검색·지도·홈 제외). 리뷰·즐겨찾기가 있을 수 있어 지우지 않는다.
 *       없으면 등록하지 않는다.</li>
 * </ul>
 * <p>모든 날짜를 빠짐없이 받았을 때만 진행 날짜를 오늘로 옮긴다. 중간에 실패하면 다음 실행이 같은 날짜부터 다시 받는다
 * (같은 수정 시각은 다시 반영해도 재조회 표시를 하지 않으므로 겹쳐 받아도 안전).
 * <p>기준 날짜가 없거나(첫 실행) {@value #MAX_INCREMENTAL_DAYS}일보다 오래 밀렸으면 조건 없이 전체를 받는다(약 101회 호출).
 */
@Slf4j
@Service
public class TourPlaceSyncService {

    static final int PAGE_SIZE = 100;
    static final int MAX_INCREMENTAL_DAYS = 31;
    private static final int MAX_PAGES_PER_QUERY = 200;
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd
    private static final String QUOTA_EXCEEDED_MARKER = "LIMITED_NUMBER_OF_SERVICE_REQUESTS";

    private final TourApiClient tourApiClient;
    private final TourApiPlaceMapper mapper;
    private final PlaceRepository placeRepository;
    private final PublicDataSyncStateRepository stateRepository;
    private final TransactionTemplate transactionTemplate;

    public TourPlaceSyncService(TourApiClient tourApiClient, TourApiPlaceMapper mapper, PlaceRepository placeRepository,
                                PublicDataSyncStateRepository stateRepository, PlatformTransactionManager transactionManager) {
        this.tourApiClient = tourApiClient;
        this.mapper = mapper;
        this.placeRepository = placeRepository;
        this.stateRepository = stateRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 마지막 동기화 날짜부터 오늘까지 바뀐 장소를 반영한다.
     *
     * @param today        오늘 날짜(한국 시간)
     * @param initialSince 저장된 진행 날짜가 없을 때 쓸 기준 날짜(예: 최초 전국 적재일). null이면 전체 동기화
     */
    public SyncResult syncChanges(LocalDate today, LocalDate initialSince) {
        LocalDate since = stateRepository.findById(PublicDataSyncState.TOUR_PET_PLACES)
                .map(PublicDataSyncState::getLastSyncedDate)
                .orElse(initialSince);

        List<String> prefixes = queryPrefixes(since, today);
        String mode = prefixes.equals(Collections.singletonList(null)) ? "전체" : since + "~" + today;
        log.info("[PLACE SYNC] start: range={}, queries={}", mode, prefixes.size());

        Counter counter = new Counter();
        String stopReason = null;
        boolean quotaExceeded = false;
        for (String prefix : prefixes) {
            try {
                syncQuery(prefix, counter);
            } catch (PublicDataException e) {
                quotaExceeded = e.getMessage() != null && e.getMessage().contains(QUOTA_EXCEEDED_MARKER);
                stopReason = "중단(" + (prefix != null ? prefix : "전체") + "): " + e.getMessage();
                log.error("[PLACE SYNC] {}", stopReason);
                break;
            }
        }

        if (stopReason == null) {
            transactionTemplate.executeWithoutResult(status -> {
                PublicDataSyncState state = stateRepository.findById(PublicDataSyncState.TOUR_PET_PLACES)
                        .orElseGet(() -> new PublicDataSyncState(PublicDataSyncState.TOUR_PET_PLACES, today));
                state.advanceTo(today);
                stateRepository.save(state);
            });
        }

        SyncResult result = new SyncResult(mode, counter.calls, counter.created, counter.changed, counter.unchanged,
                counter.hidden, counter.skipped, stopReason, quotaExceeded);
        log.info("[PLACE SYNC] done: {}", result);
        return result;
    }

    /** 날짜별 조회 조건. 기준 날짜가 없거나 너무 오래됐으면 조건 없는 전체 조회 1건([null]) */
    static List<String> queryPrefixes(LocalDate since, LocalDate today) {
        if (since == null || since.isAfter(today) || ChronoUnit.DAYS.between(since, today) > MAX_INCREMENTAL_DAYS) {
            return Collections.singletonList(null);
        }
        List<String> prefixes = new ArrayList<>();
        for (LocalDate day = since; !day.isAfter(today); day = day.plusDays(1)) {
            prefixes.add(day.format(DAY));
        }
        return prefixes;
    }

    /** 한 조건의 모든 페이지를 받아 페이지마다 저장한다 */
    private void syncQuery(String prefix, Counter counter) {
        for (int pageNo = 1; pageNo <= MAX_PAGES_PER_QUERY; pageNo++) {
            TourApiResponseDto response = tourApiClient.fetchPetPlaceChanges(PAGE_SIZE, pageNo, prefix);
            counter.calls++;
            List<TourApiPlaceItemDto> items = Optional.ofNullable(response.getResponse())
                    .map(TourApiResponseDto.Response::getBody)
                    .map(TourApiResponseDto.Body::getItemList)
                    .orElse(List.of());
            if (items.isEmpty()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> items.forEach(item -> apply(item, counter)));
            if (items.size() < PAGE_SIZE) {
                return;
            }
        }
    }

    private void apply(TourApiPlaceItemDto item, Counter counter) {
        String contentId = item.getContentId();
        if (contentId == null || contentId.isBlank()) {
            counter.skipped++;
            return;
        }
        Optional<Place> existing = placeRepository.findByPublicDataId(contentId);

        if (item.isHidden()) {
            existing.ifPresent(place -> {
                place.hideFromPublicData(ZonedDateTime.now(KOREA));
                counter.hidden++;
            });
            return;
        }

        if (existing.isEmpty()) {
            Place created = mapper.toEntity(item);
            if (created == null) { // 좌표 없음 (ADR-008)
                counter.skipped++;
                return;
            }
            created.applyPublicDataModified(item.getModifiedAt());
            placeRepository.save(created);
            counter.created++;
            return;
        }

        Place place = existing.get();
        Place mapped = mapper.toEntity(item);
        if (mapped != null) {
            place.updateDetails(mapped.getName(), mapped.getAddress(), mapped.getLocation(), mapped.getLatitude(),
                    mapped.getLongitude(), mapped.getImageUrl(), mapped.getPhone(), mapped.getCategoryName());
            mapper.applyListInfo(place, item);
        }
        if (place.applyPublicDataModified(item.getModifiedAt())) {
            counter.changed++;
        } else {
            counter.unchanged++;
        }
    }

    private static final class Counter {
        int calls;
        int created;
        int changed;
        int unchanged;
        int hidden;
        int skipped;
    }

    /**
     * @param range         조회 범위 ("전체" 또는 "시작일~오늘")
     * @param calls         TourAPI 호출 수
     * @param created       새로 등록한 장소
     * @param changed       정보가 바뀌어 정책·상세 재조회 대상이 된 장소
     * @param unchanged     이미 반영된 수정 시각이라 그대로 둔 장소
     * @param hidden        숨김 처리한 장소
     * @param skipped       ID·좌표가 없어 건너뛴 항목
     * @param stopReason    중간에 멈췄으면 사유 (이때 진행 날짜는 옮기지 않음)
     * @param quotaExceeded 일일 호출 한도 초과로 멈췄는지
     */
    public record SyncResult(String range, int calls, int created, int changed, int unchanged, int hidden, int skipped,
                             String stopReason, boolean quotaExceeded) {
        public static SyncResult notRun() {
            return new SyncResult("건너뜀(sync-place-list=false)", 0, 0, 0, 0, 0, 0, null, false);
        }
    }
}
