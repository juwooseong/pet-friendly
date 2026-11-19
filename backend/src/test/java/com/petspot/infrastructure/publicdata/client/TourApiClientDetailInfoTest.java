package com.petspot.infrastructure.publicdata.client;

import com.petspot.infrastructure.publicdata.dto.TourApiDetailCommonDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * detailCommon2 / detailIntro2 조회 검증 (2026-09-14 실제 응답 fixture 기반, 네트워크 없음).
 */
class TourApiClientDetailInfoTest {

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
    @DisplayName("detailCommon2 실제 응답에서 개요(overview)와 장소 유형(contentTypeId)을 읽는다")
    void fetchDetailCommon_Real() throws IOException {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/detailCommon2")))
                .andExpect(queryParam("contentId", "2930927"))
                .andRespond(withSuccess(fixture("success-detail-common2-real-2026-09-14.json"), MediaType.APPLICATION_JSON));

        Optional<TourApiDetailCommonDto> result = client.fetchDetailCommon("2930927");

        mockServer.verify();
        assertThat(result).isPresent();
        assertThat(result.get().getContentTypeId()).isEqualTo("38");
        assertThat(result.get().getOverview()).startsWith("가까운약국은 이웃처럼 편안한 상담과");
    }

    @Test
    @DisplayName("detailIntro2는 contentTypeId를 함께 보내고, 유형별로 다른 필드를 키-값 맵으로 돌려준다")
    void fetchDetailIntro_Real() throws IOException {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/detailIntro2")))
                .andExpect(queryParam("contentId", "2707444"))
                .andExpect(queryParam("contentTypeId", "32"))
                .andRespond(withSuccess(fixture("success-detail-intro2-lodging-real-2026-09-14.json"), MediaType.APPLICATION_JSON));

        Optional<Map<String, String>> result = client.fetchDetailIntro("2707444", "32");

        mockServer.verify();
        assertThat(result).isPresent();
        assertThat(result.get()).containsEntry("checkintime", "15:00").containsEntry("checkouttime", "10:00");
    }

    @Test
    @DisplayName("detailIntro2는 contentTypeId 없이 호출하지 않는다")
    void fetchDetailIntro_RequiresContentType() {
        assertThatThrownBy(() -> client.fetchDetailIntro("2707444", " ")).isInstanceOf(IllegalArgumentException.class);
        mockServer.verify();
    }

    private String fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/tourapi/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
