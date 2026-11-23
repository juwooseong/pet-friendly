package com.petspot.infrastructure.publicdata.client;

import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * petTourSyncList2 조회·응답 해석 (TSK-DATA-012). 2026-11-23 실제 응답 fixture 기반, 네트워크 없음.
 */
class TourApiClientSyncListTest {

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorPetTourService2";

    private MockRestServiceServer mockServer;
    private TourApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new TourApiClient(builder.build(), "test-service-key", BASE_URL);
    }

    @Test
    @DisplayName("수정 날짜 조건으로 조회하고 노출 여부(showflag)·수정 시각을 읽는다")
    void fetchPetPlaceChanges_ReadsShowFlagAndModifiedTime() throws IOException {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/petTourSyncList2")))
                .andExpect(queryParam("modifiedtime", "202611"))
                .andRespond(withSuccess(fixture("success-pet-tour-sync-list2-real-2026-11-23.json"), MediaType.APPLICATION_JSON));

        List<TourApiPlaceItemDto> items = client.fetchPetPlaceChanges(100, 1, "202611").getResponse().getBody().getItemList();

        assertThat(items).hasSize(2);
        assertThat(items.get(0).isHidden()).isFalse();
        assertThat(items.get(0).getModifiedAt())
                .isEqualTo(ZonedDateTime.of(2026, 11, 11, 15, 29, 21, 0, ZoneId.of("Asia/Seoul")));
        assertThat(items.get(1).isHidden()).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("결과가 없는 날(items가 빈 문자열)과 1건인 날(item이 객체)도 목록으로 읽는다")
    void fetchPetPlaceChanges_EmptyAndSingleItem() throws IOException {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/petTourSyncList2")))
                .andRespond(withSuccess(fixture("success-pet-tour-sync-list2-empty-real-2026-11-23.json"), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(startsWith(BASE_URL + "/petTourSyncList2")))
                .andRespond(withSuccess(fixture("success-pet-tour-sync-list2-single-item.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchPetPlaceChanges(100, 1, "20261122").getResponse().getBody().getItemList()).isEmpty();
        assertThat(client.fetchPetPlaceChanges(100, 1, "20261111").getResponse().getBody().getItemList()).hasSize(1);
    }

    private String fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/tourapi/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
