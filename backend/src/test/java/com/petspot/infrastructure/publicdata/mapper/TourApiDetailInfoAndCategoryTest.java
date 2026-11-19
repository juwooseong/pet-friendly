package com.petspot.infrastructure.publicdata.mapper;

import com.petspot.domain.place.entity.Place;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상세 부가 정보 추출, 장소 유형별 카테고리 매핑 검증.
 */
class TourApiDetailInfoAndCategoryTest {

    private final TourApiDetailInfoExtractor infoExtractor = new TourApiDetailInfoExtractor();
    private final TourApiPlaceMapper mapper = new TourApiPlaceMapper();

    @Test
    @DisplayName("홈페이지 HTML 링크에서 http(s) 주소만 꺼내고, 장소 유형별 접미사가 다른 주차·문의·유모차·요금 필드를 통합한다")
    void extract_DetailInfo() {
        Map<String, String> info = infoExtractor.extract(
                "<a href=\"https://www.example.com/park?a=1&amp;b=2\" target=\"_blank\">https://www.example.com</a>",
                Map.of("parkingshopping", "가능(20대)", "infocentershopping", "02-123-4567<br>(평일)",
                        "chkbabycarriageshopping", "대여 가능", "usefee", "무료"));

        assertThat(info)
                .containsEntry(TourApiDetailInfoExtractor.HOMEPAGE, "https://www.example.com/park?a=1&b=2")
                .containsEntry(TourApiDetailInfoExtractor.PARKING, "가능(20대)")
                .containsEntry(TourApiDetailInfoExtractor.INFO_CENTER, "02-123-4567\n(평일)")
                .containsEntry(TourApiDetailInfoExtractor.BABY_CARRIAGE, "대여 가능")
                .containsEntry(TourApiDetailInfoExtractor.USE_FEE, "무료");
    }

    @Test
    @DisplayName("http(s)가 아닌 홈페이지(javascript: 등)는 저장하지 않고, 값이 하나도 없으면 null이다")
    void extract_RejectsNonHttpAndEmpty() {
        assertThat(infoExtractor.extract("<a href=\"javascript:alert(1)\">클릭</a>", Map.of())).isNull();
        assertThat(infoExtractor.extract("", Map.of("parking", "", "roomcount", "3"))).isNull();
        assertThat(infoExtractor.extract(null, null)).isNull();
    }

    @ParameterizedTest(name = "contentTypeId {0} -> {1} / {2}")
    @CsvSource({
            "12, PARK,     관광지",
            "14, CULTURE,  문화시설",
            "15, FESTIVAL, 축제·공연·행사",
            "25, COURSE,   여행코스",
            "28, PARK,     레포츠",
            "32, HOTEL,    숙박",
            "38, SHOPPING, 쇼핑",
            "39, CAFE,     음식점·카페",
            "99, ETC,      기타 펫동반 장소",
    })
    @DisplayName("장소 유형 8종을 모두 카테고리와 읽을 수 있는 분류 이름으로 매핑한다 (예전에는 쇼핑·문화시설 등이 전부 ETC, 분류 이름은 cat3 코드)")
    void mapCategory_AllContentTypes(String contentTypeId, String category, String categoryName) {
        TourApiPlaceItemDto dto = TourApiPlaceItemDto.builder()
                .contentId("C-" + contentTypeId).contentTypeId(contentTypeId).title("장소")
                .category3("A04010600") // 사람이 읽을 수 없는 코드 - 분류 이름에 쓰지 않아야 한다
                .rawLongitude("127.0").rawLatitude("37.5")
                .firstImageThumbnail("https://tong.visitkorea.or.kr/thumb.jpg")
                .imageCopyrightType("Type3")
                .build();

        Place place = mapper.toEntity(dto);

        assertThat(place.getCategory()).isEqualTo(category);
        assertThat(place.getCategoryName()).isEqualTo(categoryName);
        assertThat(place.getContentTypeId()).isEqualTo(contentTypeId);
        assertThat(place.getThumbnailUrl()).isEqualTo("https://tong.visitkorea.or.kr/thumb.jpg");
        assertThat(place.getImageCopyrightType()).isEqualTo("Type3");
    }

    @Test
    @DisplayName("재적재 시 기존 장소(예전 매핑으로 ETC·빈 분류 이름)도 새 카테고리·썸네일로 보정된다")
    void applyListInfo_FixesPreviouslyLoadedPlace() {
        Place loadedBefore = Place.builder().publicDataId("2930927").name("약국").category("ETC").categoryName("")
                .address("주소").latitude(37.5).longitude(127.0).build();
        TourApiPlaceItemDto dto = TourApiPlaceItemDto.builder()
                .contentId("2930927").contentTypeId("38").firstImageThumbnail("https://t.jpg").imageCopyrightType("Type1").build();

        mapper.applyListInfo(loadedBefore, dto);

        assertThat(loadedBefore.getCategory()).isEqualTo("SHOPPING");
        assertThat(loadedBefore.getCategoryName()).isEqualTo("쇼핑");
        assertThat(loadedBefore.getThumbnailUrl()).isEqualTo("https://t.jpg");
    }
}
