package com.petspot.infrastructure.publicdata.mapper;

import com.petspot.domain.place.entity.Place;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourApiPlaceMapperTest {

    private TourApiPlaceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TourApiPlaceMapper();
    }

    @Test
    @DisplayName("TourApiPlaceItemDto -> Place JPA Entity 변환 및 PostGIS Point 위치 생성 검증")
    void toEntity_Success() {
        // given
        TourApiPlaceItemDto dto = TourApiPlaceItemDto.builder()
                .contentId("CONTENT-12345")
                .contentTypeId("39")
                .title("댕댕 카페")
                .address1("서울특별시 마포구 월드컵북로 100")
                .address2("2층")
                .tel("02-1234-5678")
                .longitude(126.901234)
                .latitude(37.556789)
                .firstImage("https://example.com/image.jpg")
                .petPolicyInfo("실내 목줄 필수")
                .petWeightLimitText("15kg 이하 소형견/중형견 가능")
                .category3("카페")
                .build();

        // when
        Place place = mapper.toEntity(dto);

        // then
        assertThat(place).isNotNull();
        assertThat(place.getPublicDataId()).isEqualTo("CONTENT-12345");
        assertThat(place.getName()).isEqualTo("댕댕 카페");
        assertThat(place.getCategory()).isEqualTo("CAFE");
        assertThat(place.getAddress()).isEqualTo("서울특별시 마포구 월드컵북로 100 2층");
        assertThat(place.getLongitude()).isEqualTo(126.901234);
        assertThat(place.getLatitude()).isEqualTo(37.556789);

        // PostGIS Point 좌표 검증 (x: longitude, y: latitude)
        assertThat(place.getLocation()).isNotNull();
        assertThat(place.getLocation().getSRID()).isEqualTo(4326);
        assertThat(place.getLocation().getX()).isEqualTo(126.901234);
        assertThat(place.getLocation().getY()).isEqualTo(37.556789);

        // 체중 정제 검증
        assertThat(place.getMaxWeightLimitKg()).isEqualByComparingTo("15");
    }
}
