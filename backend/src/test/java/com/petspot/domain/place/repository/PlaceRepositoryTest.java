package com.petspot.domain.place.repository;

import com.petspot.domain.place.entity.Place;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceRepositoryTest {

    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    @DisplayName("Place Entity 인스턴스 생성 및 필드 매핑 단위 테스트")
    void placeEntity_Creation_Success() {
        // given
        Point point = geometryFactory.createPoint(new Coordinate(126.9, 37.5));
        Place place = Place.builder()
                .publicDataId("TOUR-999")
                .name("테스트 펫 파크")
                .category("PARK")
                .categoryName("공원")
                .address("서울특별시 성동구 서울숲")
                .location(point)
                .latitude(37.5)
                .longitude(126.9)
                .build();

        // then
        assertThat(place.getPublicDataId()).isEqualTo("TOUR-999");
        assertThat(place.getName()).isEqualTo("테스트 펫 파크");
        assertThat(place.getCategory()).isEqualTo("PARK");
        assertThat(place.getLocation()).isEqualTo(point);
    }
}
