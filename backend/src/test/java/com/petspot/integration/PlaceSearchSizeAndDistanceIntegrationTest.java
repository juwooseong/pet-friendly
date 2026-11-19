package com.petspot.integration;

import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 반려동물 크기(sizeCategory) 필터 및 거리순 정렬(Pageable) 통합 테스트.
 * 실제 PostGIS DB에 저장된 allowed_sizes(JSONB) 컬럼과 ST_Distance 계산 결과를 검증한다.
 */
@SpringBootTest
@Transactional
class PlaceSearchSizeAndDistanceIntegrationTest {

    @Autowired
    private PlaceRepository placeRepository;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * 테스트 장소는 실제 장소가 없는 서해 바다 위 좌표(34.00N, 124.52E 부근)에 만든다. 테스트가 개발 DB를 함께 쓰므로,
     * 전국 실제 데이터(2026-11-19 적재, 9,630곳)가 검색 결과에 섞이지 않도록 검색 범위를 이 근처로 한정한다.
     */
    private static final double ISOLATED_LAT = 34.0067;
    private static final double ISOLATED_LON = 124.5236;

    @Test
    @DisplayName("정렬 기준이 모두 같은(동점) 장소가 많아도 페이지끼리 겹치거나 빠지지 않는다 - 평점순/거리순 모두")
    void searchPlaces_TiedSortKeys_PagesDoNotOverlap() {
        // given: 같은 좌표·같은 평점(리뷰 없음 0.0)·같은 리뷰 수인 장소 15곳 (2026-11-19 실제 데이터에서 발견된 상황 재현)
        String prefix = "페이징동점테스트-" + java.util.UUID.randomUUID();
        for (int i = 0; i < 15; i++) {
            placeRepository.save(Place.builder()
                    .name(prefix + "-" + i)
                    .category("ETC")
                    .address("테스트 주소")
                    .location(point(ISOLATED_LAT, ISOLATED_LON))
                    .latitude(ISOLATED_LAT)
                    .longitude(ISOLATED_LON)
                    .build());
        }

        PlaceSearchCondition byRating = PlaceSearchCondition.builder().keyword(prefix).build();
        PlaceSearchCondition byDistance = PlaceSearchCondition.builder()
                .keyword(prefix).latitude(ISOLATED_LAT).longitude(ISOLATED_LON).radiusKm(1.0).build();

        for (PlaceSearchCondition condition : List.of(byRating, byDistance)) {
            java.util.List<java.util.UUID> seen = new java.util.ArrayList<>();
            for (int page = 0; page < 3; page++) {
                placeRepository.searchPlaces(condition, PageRequest.of(page, 5))
                        .getContent().forEach(dto -> seen.add(dto.getId()));
            }
            // then: 3페이지를 합치면 중복 없이 15곳 전부
            assertThat(seen).hasSize(15).doesNotHaveDuplicates();
        }
    }

    private Point point(double lat, double lon) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
    }

    @Test
    @DisplayName("sizeCategory 조건 검색 시 allowed_sizes(JSONB)에 해당 크기가 포함된 장소만 반환된다")
    void searchPlaces_SizeCategoryFilter_ReturnsOnlyMatchingPlaces() {
        // given: SMALL만 허용하는 장소, LARGE만 허용하는 장소, 전체 허용(기본값) 장소
        placeRepository.save(Place.builder()
                .name("소형견 전용 카페")
                .category("CAFE")
                .categoryName("카페")
                .address("서울 마포구")
                .location(point(34.0067, 124.5236))
                .latitude(34.0067)
                .longitude(124.5236)
                .allowedSizes(List.of("SMALL"))
                .build());

        placeRepository.save(Place.builder()
                .name("대형견 전용 운동장")
                .category("PARK")
                .categoryName("공원")
                .address("서울 마포구")
                .location(point(34.0068, 124.5237))
                .latitude(34.0068)
                .longitude(124.5237)
                .allowedSizes(List.of("LARGE"))
                .build());

        placeRepository.save(Place.builder()
                .name("전체 견종 동반 가능 카페")
                .category("CAFE")
                .categoryName("카페")
                .address("서울 마포구")
                .location(point(34.0069, 124.5238))
                .latitude(34.0069)
                .longitude(124.5238)
                // TSK-DATA-008(ADR-007): allowedSizes 미지정 시 더 이상 전체 허용으로 자동 치환되지 않는다
                // (null = 미검증). 이 테스트는 "실제 근거로 전체 크기가 확인된 장소"를 검증하려는 것이므로
                // 명시적으로 전체 크기를 지정한다.
                .allowedSizes(List.of("SMALL", "MEDIUM", "LARGE"))
                .build());

        PlaceSearchCondition condition = PlaceSearchCondition.builder()
                .sizeCategory("SMALL")
                .latitude(ISOLATED_LAT)
                .longitude(ISOLATED_LON)
                .radiusKm(1.0)
                .build();

        // when
        List<PlaceSearchResponseDto> results = placeRepository.searchPlaces(condition);

        // then: SMALL을 허용하는 장소(소형견 전용 카페, 전체 견종 동반 가능 카페)만 반환
        assertThat(results).extracting(PlaceSearchResponseDto::getName)
                .containsExactlyInAnyOrder("소형견 전용 카페", "전체 견종 동반 가능 카페");
        assertThat(results).noneMatch(dto -> dto.getName().equals("대형견 전용 운동장"));
    }

    /**
     * TSK-DATA-008 (ADR-007): allowedSizes/isIndoorAllowed 등 정책 필드가 null(미검증)인 장소를
     * 저장/조회해도 예외 없이 동작하고, 크기 필터 검색에서는 근거가 없으므로 매칭되지 않아야 한다
     * (jsonb_exists(NULL, ...)는 NULL을 반환하므로 WHERE 절에서 자동으로 제외된다).
     */
    @Test
    @DisplayName("반려동물 정책 필드가 null인 장소도 저장/상세조회는 정상 동작하고, 크기 필터 검색에서는 제외된다")
    void searchPlaces_PolicyFieldsNull_PersistsSafelyAndExcludedFromSizeFilter() {
        // given: 정책 정보가 전혀 검증되지 않은 장소(TourAPI 기본정보만 있는 상태를 재현)
        Place savedPlace = placeRepository.save(Place.builder()
                .name("정책 미검증 장소")
                .category("ETC")
                .categoryName("기타")
                .address("서울 마포구")
                .location(point(34.0070, 124.5239))
                .latitude(34.0070)
                .longitude(124.5239)
                .build()); // allowedSizes/isIndoorAllowed/isOutdoorAllowed/hasOffLeashZone/maxWeightLimitKg 전부 미지정 -> null

        // then: 저장 자체는 예외 없이 성공하고 null이 그대로 유지된다
        assertThat(savedPlace.getAllowedSizes()).isNull();
        assertThat(savedPlace.getIsIndoorAllowed()).isNull();
        assertThat(savedPlace.getIsOutdoorAllowed()).isNull();
        assertThat(savedPlace.getHasOffLeashZone()).isNull();
        assertThat(savedPlace.getMaxWeightLimitKg()).isNull();

        // when: 상세 조회(findPlaceDetail) - JSONB null 컬럼을 읽어도 예외가 발생하지 않아야 한다
        var detail = placeRepository.findPlaceDetail(savedPlace.getId());
        assertThat(detail).isPresent();
        assertThat(detail.get().getAllowedSizes()).isNull();
        assertThat(detail.get().getMaxWeightLimitKg()).isNull();

        // when: SMALL 크기 필터로 검색 - 근거(allowedSizes) 없는 장소는 결과에 포함되지 않아야 한다
        PlaceSearchCondition condition = PlaceSearchCondition.builder()
                .sizeCategory("SMALL")
                .latitude(ISOLATED_LAT)
                .longitude(ISOLATED_LON)
                .radiusKm(1.0)
                .build();
        List<PlaceSearchResponseDto> results = placeRepository.searchPlaces(condition);

        assertThat(results).noneMatch(dto -> dto.getName().equals("정책 미검증 장소"));
    }

    @Test
    @DisplayName("위도/경도 조건 검색 시 DB에서 계산된 거리(distanceKm) 오름차순으로 정렬되고 Pageable과 함께 동작한다")
    void searchPlaces_DistanceSort_WithPagination() {
        // given: 기준 좌표(34.0067, 124.5236)로부터 거리가 서로 다른 3개 장소
        double baseLat = 34.0067;
        double baseLon = 124.5236;

        placeRepository.save(Place.builder()
                .name("가장 먼 장소")
                .category("CAFE")
                .categoryName("카페")
                .address("서울 마포구")
                .location(point(baseLat + 0.03, baseLon + 0.03))
                .latitude(baseLat + 0.03)
                .longitude(baseLon + 0.03)
                .build());

        placeRepository.save(Place.builder()
                .name("가장 가까운 장소")
                .category("CAFE")
                .categoryName("카페")
                .address("서울 마포구")
                .location(point(baseLat + 0.001, baseLon + 0.001))
                .latitude(baseLat + 0.001)
                .longitude(baseLon + 0.001)
                .build());

        placeRepository.save(Place.builder()
                .name("중간 거리 장소")
                .category("CAFE")
                .categoryName("카페")
                .address("서울 마포구")
                .location(point(baseLat + 0.01, baseLon + 0.01))
                .latitude(baseLat + 0.01)
                .longitude(baseLon + 0.01)
                .build());

        PlaceSearchCondition condition = PlaceSearchCondition.builder()
                .latitude(baseLat)
                .longitude(baseLon)
                .radiusKm(10.0)
                .build();
        Pageable pageable = PageRequest.of(0, 2);

        // when: 1페이지(size=2)
        Page<PlaceSearchResponseDto> firstPage = placeRepository.searchPlaces(condition, pageable);

        // then: 총 3건, 거리 오름차순으로 가까운 장소 2개가 1페이지에 포함
        assertThat(firstPage.getTotalElements()).isEqualTo(3L);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getContent().get(0).getName()).isEqualTo("가장 가까운 장소");
        assertThat(firstPage.getContent().get(1).getName()).isEqualTo("중간 거리 장소");
        assertThat(firstPage.getContent().get(0).getDistanceKm())
                .isLessThan(firstPage.getContent().get(1).getDistanceKm());
        assertThat(firstPage.hasNext()).isTrue();

        // when: 2페이지 - 남은 가장 먼 장소가 마지막에 포함되는지 확인
        Page<PlaceSearchResponseDto> secondPage = placeRepository.searchPlaces(condition, PageRequest.of(1, 2));
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().get(0).getName()).isEqualTo("가장 먼 장소");
        assertThat(secondPage.hasNext()).isFalse();
    }
}
