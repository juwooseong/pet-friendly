package com.petspot.infrastructure.publicdata.service;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiDetailCommonDto;
import com.petspot.infrastructure.publicdata.dto.TourApiImageItemDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import com.petspot.infrastructure.publicdata.mapper.TourApiDetailInfoExtractor;
import com.petspot.infrastructure.publicdata.mapper.TourApiOperatingHoursExtractor;
import com.petspot.infrastructure.publicdata.mapper.TourApiText;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 장소 상세 화면을 처음 열 때 TourAPI에서 장소 소개와 운영시간을 가져와 저장한다 (지연 적재).
 *
 * <p>설명/운영시간은 장소마다 상세 API를 1~2회 호출해야 해서 전국 9천여 곳을 미리 채우면 일일 트래픽 한도를 크게 쓴다.
 * 대신 사용자가 실제로 보는 장소만, 처음 볼 때 한 번 가져와 {@code detailSyncedAt}을 기록하고 이후에는 DB 값을 쓴다.
 *
 * <ol>
 *   <li>{@code detailCommon2} → 개요(overview), 홈페이지, 장소 유형(contentTypeId)</li>
 *   <li>{@code detailIntro2}(contentTypeId 필요) → 운영시간/쉬는 날, 주차·문의처·유모차·이용 요금</li>
 *   <li>{@code detailImage2} → 상세 사진 목록(최대 10장, 공공누리 유형 포함)</li>
 * </ol>
 *
 * <p><b>실패해도 상세 화면은 그대로 보여준다:</b> TourAPI 오류(한도 초과·장애 등)는 경고 로그만 남기고 조회 시각을
 * 기록하지 않으므로 다음 조회 때 다시 시도된다. 외부 호출은 트랜잭션 밖에서 하고, 저장만 별도 트랜잭션으로 한다.
 */
@Slf4j
@Service
public class PlaceDetailEnrichmentService {

    private final TourApiClient tourApiClient;
    /** 저장할 상세 사진 최대 장수 */
    static final int MAX_GALLERY_IMAGES = 10;

    private final TourApiOperatingHoursExtractor hoursExtractor;
    private final TourApiDetailInfoExtractor detailInfoExtractor;
    private final PlaceRepository placeRepository;
    private final TransactionTemplate writeTransaction;

    public PlaceDetailEnrichmentService(TourApiClient tourApiClient, TourApiOperatingHoursExtractor hoursExtractor,
                                        TourApiDetailInfoExtractor detailInfoExtractor,
                                        PlaceRepository placeRepository, PlatformTransactionManager transactionManager) {
        this.tourApiClient = tourApiClient;
        this.hoursExtractor = hoursExtractor;
        this.detailInfoExtractor = detailInfoExtractor;
        this.placeRepository = placeRepository;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        // 호출부가 읽기 전용 트랜잭션 안이어도 저장이 반영되도록 항상 새 트랜잭션에서 쓴다
        this.writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 필요한 경우에만 상세 정보를 가져와 저장한다. 어떤 경우에도 예외를 던지지 않는다.
     *
     * @return TourAPI를 호출해 저장했으면 true
     */
    public boolean enrichIfNeeded(UUID placeId) {
        Optional<Place> found = placeRepository.findById(placeId);
        if (found.isEmpty() || !found.get().needsTourApiDetail()) {
            return false;
        }
        String contentId = found.get().getPublicDataId();

        String description;
        String operatingHours = null;
        Map<String, String> detailInfo;
        List<Map<String, String>> gallery;
        try {
            Optional<TourApiDetailCommonDto> common = tourApiClient.fetchDetailCommon(contentId);
            description = common.map(c -> TourApiText.clean(c.getOverview())).orElse(null);

            Map<String, String> introFields = null;
            String contentTypeId = common.map(TourApiDetailCommonDto::getContentTypeId).orElse(null);
            if (contentTypeId != null && !contentTypeId.isBlank()) {
                introFields = tourApiClient.fetchDetailIntro(contentId, contentTypeId).orElse(null);
                operatingHours = hoursExtractor.extract(introFields);
            }
            detailInfo = detailInfoExtractor.extract(common.map(TourApiDetailCommonDto::getHomepage).orElse(null), introFields);
            gallery = toGallery(tourApiClient.fetchDetailImages(contentId));
        } catch (PublicDataException | IllegalArgumentException e) {
            log.warn("[PLACE DETAIL ENRICH] TourAPI 상세 조회 실패 - 다음 조회 때 재시도: placeId={}, contentId={}, reason={}",
                    placeId, contentId, e.getMessage());
            return false;
        }

        String finalDescription = description;
        String finalOperatingHours = operatingHours;
        Map<String, String> finalDetailInfo = detailInfo;
        List<Map<String, String>> finalGallery = gallery;
        try {
            writeTransaction.executeWithoutResult(status -> placeRepository.findById(placeId).ifPresent(
                    place -> place.applyTourApiDetail(finalDescription, finalOperatingHours, finalDetailInfo,
                            finalGallery, ZonedDateTime.now())));
        } catch (RuntimeException e) {
            log.warn("[PLACE DETAIL ENRICH] 저장 실패 - 다음 조회 때 재시도: placeId={}, reason={}", placeId, e.getMessage());
            return false;
        }
        log.info("[PLACE DETAIL ENRICH] saved: placeId={}, description={}, operatingHours={}, detailInfo={}, images={}",
                placeId, finalDescription != null, finalOperatingHours != null,
                finalDetailInfo == null ? 0 : finalDetailInfo.size(), finalGallery == null ? 0 : finalGallery.size());
        return true;
    }

    /** 사진 목록을 저장 형태로 바꾼다. http(s) 주소만 남기고 최대 {@value #MAX_GALLERY_IMAGES}장 */
    static List<Map<String, String>> toGallery(List<TourApiImageItemDto> images) {
        List<Map<String, String>> gallery = new ArrayList<>();
        for (TourApiImageItemDto image : images) {
            String url = httpOnly(image.getOriginalUrl());
            if (url == null) {
                continue;
            }
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("url", url);
            String small = httpOnly(image.getSmallUrl());
            entry.put("thumbnailUrl", small != null ? small : url);
            if (image.getCopyrightType() != null && !image.getCopyrightType().isBlank()) {
                entry.put("copyrightType", image.getCopyrightType().trim());
            }
            gallery.add(entry);
            if (gallery.size() >= MAX_GALLERY_IMAGES) {
                break;
            }
        }
        return gallery.isEmpty() ? null : gallery;
    }

    private static String httpOnly(String url) {
        return url != null && (url.startsWith("https://") || url.startsWith("http://")) ? url.trim() : null;
    }
}
