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
                .rawLongitude("126.901234")
                .rawLatitude("37.556789")
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

    @Test
    @DisplayName("mapx/mapy가 빈 문자열(좌표 누락)인 경우 예외 없이 0.0 좌표로 매핑되어야 한다")
    void toEntity_BlankCoordinates_DefaultsToZero() {
        // given
        TourApiPlaceItemDto dto = TourApiPlaceItemDto.builder()
                .contentId("NO-COORD-1")
                .title("좌표 누락 장소")
                .address1("경기도 용인시 기흥구")
                .rawLongitude("")
                .rawLatitude("")
                .build();

        // when
        Place place = mapper.toEntity(dto);

        // then
        assertThat(place.getLongitude()).isEqualTo(0.0);
        assertThat(place.getLatitude()).isEqualTo(0.0);
        assertThat(place.getLocation()).isNotNull();
    }

    @Test
    @DisplayName("mapx/mapy가 숫자로 파싱 불가능한 문자열인 경우 예외 없이 0.0 좌표로 매핑되어야 한다")
    void toEntity_MalformedCoordinates_DefaultsToZero() {
        // given
        TourApiPlaceItemDto dto = TourApiPlaceItemDto.builder()
                .contentId("BAD-COORD-1")
                .title("잘못된 좌표 장소")
                .rawLongitude("N/A")
                .rawLatitude("알수없음")
                .build();

        // when
        Place place = mapper.toEntity(dto);

        // then
        assertThat(place.getLongitude()).isEqualTo(0.0);
        assertThat(place.getLatitude()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("체중 제한 텍스트에 kg 표기가 없으면 maxWeightLimitKg는 null이어야 한다")
    void toEntity_NoWeightLimitText_ReturnsNull() {
        TourApiPlaceItemDto dto = TourApiPlaceItemDto.builder()
                .contentId("NO-WEIGHT-1")
                .title("체중 정보 없는 장소")
                .petWeightLimitText("소형견만 가능")
                .build();

        Place place = mapper.toEntity(dto);

        assertThat(place.getMaxWeightLimitKg()).isNull();
    }
}
