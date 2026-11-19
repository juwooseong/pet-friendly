package com.petspot.infrastructure.publicdata.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 장소 소개/운영시간 텍스트 정리 검증. 운영시간 입력값은 실제 detailIntro2 응답(2026-09-14 fixture)의 값이다.
 */
class TourApiTextAndHoursTest {

    private final TourApiOperatingHoursExtractor extractor = new TourApiOperatingHoursExtractor();

    @Test
    @DisplayName("HTML 조각은 줄바꿈만 살리고 태그·엔티티를 제거한 평문이 된다 (화면은 텍스트로만 렌더링)")
    void clean_StripsHtml() {
        String raw = "반려견 동반<br>가능한 숲길&nbsp;입니다.<br /><br/><br>"
                + "<a href=\"http://example.com\">홈페이지</a> &amp; <script>alert(1)</script>안내";

        assertThat(TourApiText.clean(raw)).isEqualTo("반려견 동반\n가능한 숲길 입니다.\n\n홈페이지 & alert(1)안내");
    }

    @Test
    @DisplayName("빈 값/공백/태그뿐인 값은 null이다")
    void clean_BlankIsNull() {
        assertThat(TourApiText.clean(null)).isNull();
        assertThat(TourApiText.clean("  ")).isNull();
        assertThat(TourApiText.clean("<br><br/>")).isNull();
    }

    @Test
    @DisplayName("쇼핑(contentTypeId 38) 실제 응답: opentime만 있고 쉬는 날이 빈 값이면 이용 시간만 표시한다")
    void hours_Shopping() {
        assertThat(extractor.extract(Map.of("contentid", "2930927", "opentime", "09:00-20:00", "restdateshopping", "")))
                .isEqualTo("이용 시간: 09:00-20:00");
    }

    @Test
    @DisplayName("숙박(contentTypeId 32) 실제 응답: 체크인/체크아웃 시간을 표시한다")
    void hours_Lodging() {
        assertThat(extractor.extract(Map.of("checkintime", "15:00", "checkouttime", "10:00")))
                .isEqualTo("체크인 15:00 / 체크아웃 10:00");
    }

    @Test
    @DisplayName("관광지: 이용 시간과 쉬는 날을 줄을 나눠 표시하고 HTML 줄바꿈도 정리한다")
    void hours_Attraction() {
        assertThat(extractor.extract(Map.of("usetime", "09:00~18:00<br>(입장 마감 17:00)", "restdate", "매주 월요일")))
                .isEqualTo("이용 시간: 09:00~18:00\n(입장 마감 17:00)\n쉬는 날: 매주 월요일");
    }

    @Test
    @DisplayName("운영시간 관련 값이 하나도 없으면 null이다")
    void hours_None() {
        assertThat(extractor.extract(Map.of("roomcount", "10"))).isNull();
        assertThat(extractor.extract(null)).isNull();
    }
}
