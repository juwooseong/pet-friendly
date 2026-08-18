package com.petspot.integration;

import com.petspot.application.place.PlaceQueryService;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.global.error.exception.PlaceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 장소 상세 조회(TSK-BE-029) 실PostGIS DB 통합 테스트.
 * QueryDSL Projection이 실제 DB 컬럼(allowed_sizes JSONB 포함)을 올바르게 매핑하는지 검증한다.
 */
@SpringBootTest
@Transactional
class PlaceDetailIntegrationTest {

    @Autowired
    private PlaceQueryService placeQueryService;

    @Autowired
    private PlaceRepository placeRepository;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    @DisplayName("존재하는 장소 ID로 상세 조회 시 필요한 필드가 모두 채워진 DTO를 반환한다")
    void getPlaceDetail_ExistingId_ReturnsDetail() {
        // given
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(126.9236, 37.5567));
        Place saved = placeRepository.save(Place.builder()
                .name("홍대 애견카페")
                .category("CAFE")
                .categoryName("카페")
                .address("서울특별시 마포구 홍익로 10")
                .location(point)
                .latitude(37.5567)
                .longitude(126.9236)
                .phone("02-123-4567")
                .imageUrl("https://example.com/image.jpg")
                .rating(BigDecimal.valueOf(4.5))
                .reviewCount(10)
                .allowedSizes(List.of("SMALL", "MEDIUM"))
                .build());

        // when
        PlaceSearchResponseDto result = placeQueryService.getPlaceDetail(saved.getId());

        // then
        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getName()).isEqualTo("홍대 애견카페");
        assertThat(result.getAddress()).isEqualTo("서울특별시 마포구 홍익로 10");
        assertThat(result.getLatitude()).isEqualTo(37.5567);
        assertThat(result.getLongitude()).isEqualTo(126.9236);
        assertThat(result.getPhone()).isEqualTo("02-123-4567");
        assertThat(result.getImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(result.getCategory()).isEqualTo("CAFE");
        assertThat(result.getAllowedSizes()).containsExactlyInAnyOrder("SMALL", "MEDIUM");
        assertThat(result.getRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
        assertThat(result.getReviewCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("존재하지 않는 장소 ID로 상세 조회 시 PlaceNotFoundException이 발생한다")
    void getPlaceDetail_NonExistingId_ThrowsNotFound() {
        // given
        UUID randomId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> placeQueryService.getPlaceDetail(randomId))
                .isInstanceOf(PlaceNotFoundException.class);
    }
}
