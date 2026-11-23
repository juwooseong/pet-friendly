package com.petspot.domain.place.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceCoverImageTest {

    @Test
    @DisplayName("상세 사진으로 채운 대표 사진은 사진 없는 목록 갱신으로 지워지지 않고, 원래 대표 사진이 있으면 덮어쓰지 않는다 (TSK-DATA-013)")
    void coverFromGallery_KeptOnListUpdateWithoutImage() {
        Place noCover = Place.builder().name("숙소").category("HOTEL").build();
        noCover.applyTourApiDetail(null, null, null,
                List.of(Map.of("url", "https://t/1.jpg", "thumbnailUrl", "https://t/1s.jpg", "copyrightType", "Type3")),
                ZonedDateTime.now());
        noCover.applyTourApiListInfo("HOTEL", "숙박", "32", null, null); // 목록에 사진 없음

        assertThat(noCover.getImageUrl()).isEqualTo("https://t/1.jpg");
        assertThat(noCover.getThumbnailUrl()).isEqualTo("https://t/1s.jpg");
        assertThat(noCover.getImageCopyrightType()).isEqualTo("Type3");

        Place withCover = Place.builder().name("공원").category("PARK").imageUrl("https://t/cover.jpg").build();
        withCover.applyTourApiDetail(null, null, null, List.of(Map.of("url", "https://t/1.jpg")), ZonedDateTime.now());

        assertThat(withCover.getImageUrl()).isEqualTo("https://t/cover.jpg");
    }
}
