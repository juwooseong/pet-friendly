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

    /**
     * TSK-DATA-003-B (ADR-008, 전략 B): 좌표를 파싱할 수 없는 장소는 임의 좌표(0.0/0.0)로 대체하지 않고
     * 아예 매핑하지 않는다({@code null} 반환) - Place의 좌표 컬럼이 DB NOT NULL이라 실제 좌표 없이는
     * 유효한 Entity를 만들 수 없고, 임의 좌표를 채우면 지도/거리 검색에 잘못된 위치로 노출되기 때문이다.
     */
    @Test
    @DisplayName("mapx/mapy가 빈 문자열(좌표 누락)인 경우 임의 좌표로 대체하지 않고 null을 반환해야 한다")
    void toEntity_BlankCoordinates_ReturnsNull() {
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
        assertThat(place).isNull();
    }

    @Test
    @DisplayName("mapx/mapy가 숫자로 파싱 불가능한 문자열인 경우 임의 좌표로 대체하지 않고 null을 반환해야 한다")
    void toEntity_MalformedCoordinates_ReturnsNull() {
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
        assertThat(place).isNull();
    }

    @Test
    @DisplayName("체중 제한 텍스트에 kg 표기가 없으면 maxWeightLimitKg는 null이어야 한다")
    void toEntity_NoWeightLimitText_ReturnsNull() {
        TourApiPlaceItemDto dto = TourApiPlaceItemDto.builder()
                .contentId("NO-WEIGHT-1")
                .title("체중 정보 없는 장소")
                .petWeightLimitText("소형견만 가능")
                .rawLongitude("127.0")
                .rawLatitude("37.5")
                .build();

        Place place = mapper.toEntity(dto);

        assertThat(place).isNotNull();
        assertThat(place.getMaxWeightLimitKg()).isNull();
    }

    /**
     * TSK-DATA-008 (ADR-007): TourAPI가 실제로 제공하는 장소 기본정보만 담은 DTO(정책 관련 필드 전부
     * 미지정)로 매핑했을 때, Place의 반려동물 정책 필드가 근거 없이 채워지지 않고 전부 null(미검증)로
     * 유지되어야 한다. TSK-DATA-005에서 실제 검증된 areaBasedList2/detailIntro2/detailInfo2/detailCommon2
     * 응답에는 이 정책 필드들을 채울 근거가 없다.
     */
    @Test
    @DisplayName("TourAPI 기본 장소 데이터만 들어온 경우 반려동물 정책 필드는 전부 null(미검증)이어야 한다")
    void toEntity_BasicPlaceDataOnly_PolicyFieldsRemainNull() {
        // given: TSK-DATA-005 실제 검증 결과와 동일하게, 정책 관련 필드는 전혀 채우지 않은 DTO
        TourApiPlaceItemDto dto = TourApiPlaceItemDto.builder()
                .contentId("BASIC-1")
                .contentTypeId("38")
                .title("기본정보만 있는 장소")
                .address1("부산광역시 부산진구 가야대로 779")
                .tel("051-819-1421")
                .rawLongitude("129.056805686045")
                .rawLatitude("35.1579550616706")
                .firstImage("https://example.com/basic.jpg")
                .build();

        // when
        Place place = mapper.toEntity(dto);

        // then: 장소 기본정보는 정상 매핑되지만
        assertThat(place.getPublicDataId()).isEqualTo("BASIC-1");
        assertThat(place.getName()).isEqualTo("기본정보만 있는 장소");
        assertThat(place.getAddress()).isEqualTo("부산광역시 부산진구 가야대로 779");
        assertThat(place.getLongitude()).isEqualTo(129.056805686045);
        assertThat(place.getLatitude()).isEqualTo(35.1579550616706);
        assertThat(place.getImageUrl()).isEqualTo("https://example.com/basic.jpg");

        // 반려동물 정책 필드는 근거 없이 채워지지 않고 전부 null이어야 한다
        assertThat(place.getMaxWeightLimitKg()).isNull();
        assertThat(place.getAllowedSizes()).isNull();
        assertThat(place.getIsIndoorAllowed()).isNull();
        assertThat(place.getIsOutdoorAllowed()).isNull();
        assertThat(place.getHasOffLeashZone()).isNull();
    }

    /**
     * TSK-DATA-008: "전체 크기 허용" 같은 임의의 기본값이 더 이상 자동으로 채워지지 않음을 명시적으로
     * 검증한다(과거 버그: allowedSizes 미지정 시 ["SMALL","MEDIUM","LARGE"]로 자동 치환되었음).
     */
    @Test
    @DisplayName("정책 정보가 없는 응답에서도 allowedSizes에 전체 크기 허용 같은 기본값이 자동 생성되지 않는다")
    void toEntity_NoPolicyInfo_DoesNotAutoFillAllowedSizes() {
        TourApiPlaceItemDto dto = TourApiPlaceItemDto.builder()
                .contentId("NO-POLICY-1")
                .title("정책 정보 없는 숙박 장소")
                .contentTypeId("32")
                .rawLongitude("127.0")
                .rawLatitude("37.5")
                .build();

        Place place = mapper.toEntity(dto);

        assertThat(place).isNotNull();
        assertThat(place.getAllowedSizes()).isNull();
        assertThat(place.getIsIndoorAllowed()).isNull();
        assertThat(place.getIsOutdoorAllowed()).isNull();
        assertThat(place.getHasOffLeashZone()).isNull();
    }
}
