package com.petspot.infrastructure.publicdata.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourApiImageUrlsTest {

    @Test
    @DisplayName("관광공사 사진 서버 http 주소만 https로 바꾸고, 빈 값·http(s)가 아닌 값은 null")
    void normalize() {
        assertThat(TourApiImageUrls.normalize(" http://tong.visitkorea.or.kr/cms/a.jpg "))
                .isEqualTo("https://tong.visitkorea.or.kr/cms/a.jpg");
        assertThat(TourApiImageUrls.normalize("https://tong.visitkorea.or.kr/cms/a.jpg"))
                .isEqualTo("https://tong.visitkorea.or.kr/cms/a.jpg");
        assertThat(TourApiImageUrls.normalize("http://other.example.com/a.jpg")).isEqualTo("http://other.example.com/a.jpg");
        assertThat(TourApiImageUrls.normalize("")).isNull();
        assertThat(TourApiImageUrls.normalize("javascript:alert(1)")).isNull();
    }
}
