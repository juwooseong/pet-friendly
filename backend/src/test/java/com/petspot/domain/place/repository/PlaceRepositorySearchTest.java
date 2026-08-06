package com.petspot.domain.place.repository;

import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import com.petspot.domain.place.entity.Place;
import com.petspot.infrastructure.publicdata.mapper.TourApiPlaceMapper;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class PlaceRepositorySearchTest {

    @Mock
    private PlaceRepository placeRepository;

    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("1. 사용자 위치 기준 3.0km 이내 반경 검색 및 거리 정렬 Mock 테스트")
    void searchPlaces_Radius_Success() {
        // given
        PlaceSearchCondition condition = PlaceSearchCondition.builder()
                .latitude(37.5567)
                .longitude(126.9236)
                .radiusKm(3.0)
                .build();

        PlaceSearchResponseDto dto1 = new PlaceSearchResponseDto(
                java.util.UUID.randomUUID(), "P-101", "홍대 멍멍 카페", "CAFE", "카페",
                "서울특별시 마포구 홍익로 10", 37.5567, 126.9236, "02-111-2222", "10:00-22:00",
                "https://example.com/img1.jpg", java.math.BigDecimal.valueOf(4.8), 15,
                java.math.BigDecimal.valueOf(10.0), 0.05
        );

        PlaceSearchResponseDto dto2 = new PlaceSearchResponseDto(
                java.util.UUID.randomUUID(), "P-102", "합정 애견 공원", "PARK", "공원",
                "서울특별시 마포구 합정동 100", 37.5495, 126.9138, "02-333-4444", "24시간",
                "https://example.com/img2.jpg", java.math.BigDecimal.valueOf(5.0), 42,
                java.math.BigDecimal.valueOf(30.0), 1.25
        );

        given(placeRepository.searchPlaces(condition)).willReturn(List.of(dto1, dto2));

        // when
        List<PlaceSearchResponseDto> results = placeRepository.searchPlaces(condition);

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("홍대 멍멍 카페");
        assertThat(results.get(0).getDistanceKm()).isEqualTo(0.05);
        assertThat(results.get(1).getName()).isEqualTo("합정 애견 공원");
    }

    @Test
    @DisplayName("2. 카테고리(CAFE) 필터링 검색 Mock 테스트")
    void searchPlaces_Category_Success() {
        // given
        PlaceSearchCondition condition = PlaceSearchCondition.builder()
                .category("CAFE")
                .build();

        PlaceSearchResponseDto dto1 = new PlaceSearchResponseDto(
                java.util.UUID.randomUUID(), "P-101", "홍대 멍멍 카페", "CAFE", "카페",
                "서울특별시 마포구 홍익로 10", 37.5567, 126.9236, "02-111-2222", "10:00-22:00",
                "https://example.com/img1.jpg", java.math.BigDecimal.valueOf(4.8), 15,
                java.math.BigDecimal.valueOf(10.0), null
        );

        given(placeRepository.searchPlaces(condition)).willReturn(List.of(dto1));

        // when
        List<PlaceSearchResponseDto> results = placeRepository.searchPlaces(condition);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("홍대 멍멍 카페");
        assertThat(results.get(0).getCategory()).isEqualTo("CAFE");
    }

    @Test
    @DisplayName("3. 키워드 검색(장소명/주소 LIKE) Mock 테스트")
    void searchPlaces_Keyword_Success() {
        // given
        PlaceSearchCondition condition = PlaceSearchCondition.builder()
                .keyword("마포구")
                .build();

        PlaceSearchResponseDto dto1 = new PlaceSearchResponseDto(
                java.util.UUID.randomUUID(), "P-101", "홍대 멍멍 카페", "CAFE", "카페",
                "서울특별시 마포구 홍익로 10", 37.5567, 126.9236, "02-111-2222", "10:00-22:00",
                "https://example.com/img1.jpg", java.math.BigDecimal.valueOf(4.8), 15,
                java.math.BigDecimal.valueOf(10.0), null
        );

        given(placeRepository.searchPlaces(condition)).willReturn(List.of(dto1));

        // when
        List<PlaceSearchResponseDto> results = placeRepository.searchPlaces(condition);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAddress()).contains("마포구");
    }

    @Test
    @DisplayName("4. 위치 + 카테고리 + 키워드 복합 조건 검색 Mock 테스트")
    void searchPlaces_MultiFilter_Success() {
        // given
        PlaceSearchCondition condition = PlaceSearchCondition.builder()
                .latitude(37.5567)
                .longitude(126.9236)
                .radiusKm(5.0)
                .category("PARK")
                .keyword("합정")
                .build();

        PlaceSearchResponseDto dto2 = new PlaceSearchResponseDto(
                java.util.UUID.randomUUID(), "P-102", "합정 애견 공원", "PARK", "공원",
                "서울특별시 마포구 합정동 100", 37.5495, 126.9138, "02-333-4444", "24시간",
                "https://example.com/img2.jpg", java.math.BigDecimal.valueOf(5.0), 42,
                java.math.BigDecimal.valueOf(30.0), 1.25
        );

        given(placeRepository.searchPlaces(condition)).willReturn(List.of(dto2));

        // when
        List<PlaceSearchResponseDto> results = placeRepository.searchPlaces(condition);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("합정 애견 공원");
    }

    @Test
    @DisplayName("5. Pageable 페이징 적용 반경 검색 Mock 테스트")
    void searchPlaces_Pageable_Success() {
        // given
        PlaceSearchCondition condition = PlaceSearchCondition.builder()
                .latitude(37.5567)
                .longitude(126.9236)
                .radiusKm(3.0)
                .build();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        PlaceSearchResponseDto dto1 = new PlaceSearchResponseDto(
                java.util.UUID.randomUUID(), "P-101", "홍대 멍멍 카페", "CAFE", "카페",
                "서울특별시 마포구 홍익로 10", 37.5567, 126.9236, "02-111-2222", "10:00-22:00",
                "https://example.com/img1.jpg", java.math.BigDecimal.valueOf(4.8), 15,
                java.math.BigDecimal.valueOf(10.0), 0.05
        );

        org.springframework.data.domain.Page<PlaceSearchResponseDto> pageResult =
                new org.springframework.data.domain.PageImpl<>(List.of(dto1), pageable, 1L);

        given(placeRepository.searchPlaces(condition, pageable)).willReturn(pageResult);

        // when
        org.springframework.data.domain.Page<PlaceSearchResponseDto> result = placeRepository.searchPlaces(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
    }
}
