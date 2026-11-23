package com.petspot.integration;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiDetailCommonDto;
import com.petspot.infrastructure.publicdata.dto.TourApiImageItemDto;
import com.petspot.infrastructure.publicdata.service.TourPlaceImageBackfillService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 대표 사진 보강 실DB 통합 테스트 (TSK-DATA-013). TourAPI 호출만 Mock.
 * <p>상세 저장이 별도 트랜잭션(REQUIRES_NEW)이라 테스트 트랜잭션 롤백을 쓰지 않고, 만든 장소를 직접 지운다.
 */
@SpringBootTest
class TourPlaceImageBackfillIntegrationTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private final String prefix = "imgfill-" + UUID.randomUUID();

    @Autowired
    private TourPlaceImageBackfillService backfillService;

    @Autowired
    private PlaceRepository placeRepository;

    @MockBean
    private TourApiClient tourApiClient;

    @AfterEach
    void cleanUp() {
        placeRepository.deleteAll(placeRepository.findAll().stream()
                .filter(p -> p.getPublicDataId() != null && p.getPublicDataId().startsWith(prefix)).toList());
    }

    @Test
    @DisplayName("대표 사진이 없는 장소만 상세를 받아 첫 상세 사진(관광공사 http 주소는 https로)을 대표 사진으로 쓴다")
    void backfill_UsesFirstGalleryImageAsCover() {
        Place noImage = placeRepository.save(place(prefix + "-a", null));
        Place withImage = placeRepository.save(place(prefix + "-b", "https://tong.visitkorea.or.kr/cover.jpg"));
        given(tourApiClient.fetchDetailCommon(anyString())).willReturn(Optional.empty());
        given(tourApiClient.fetchDetailCommon(noImage.getPublicDataId())).willReturn(Optional.of(
                TourApiDetailCommonDto.builder().contentId(noImage.getPublicDataId()).overview("소개").build()));
        given(tourApiClient.fetchDetailImages(noImage.getPublicDataId())).willReturn(List.of(
                TourApiImageItemDto.builder().originalUrl("http://tong.visitkorea.or.kr/first.jpg")
                        .smallUrl("http://tong.visitkorea.or.kr/first_s.jpg").copyrightType("Type1").build(),
                TourApiImageItemDto.builder().originalUrl("https://tong.visitkorea.or.kr/second.jpg").build()));

        TourPlaceImageBackfillService.BackfillResult result = backfillService.backfill(100000, 0);

        Place filled = placeRepository.findById(noImage.getId()).orElseThrow();
        assertThat(filled.getImageUrl()).isEqualTo("https://tong.visitkorea.or.kr/first.jpg");
        assertThat(filled.getThumbnailUrl()).isEqualTo("https://tong.visitkorea.or.kr/first_s.jpg");
        assertThat(filled.getImageCopyrightType()).isEqualTo("Type1");
        assertThat(filled.getDetailSyncedAt()).isNotNull(); // 다음 실행에서 다시 고르지 않음
        assertThat(result.withImage()).isGreaterThanOrEqualTo(1);
        verify(tourApiClient, never()).fetchDetailImages(withImage.getPublicDataId()); // 사진 있는 장소는 대상 아님
    }

    private Place place(String contentId, String imageUrl) {
        return Place.builder().publicDataId(contentId).name(contentId).category("HOTEL").address("테스트 주소")
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(124.5236, 34.0067)))
                .latitude(34.0067).longitude(124.5236).imageUrl(imageUrl).build();
    }
}
