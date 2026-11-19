package com.petspot.infrastructure.publicdata.client;

import com.petspot.infrastructure.publicdata.dto.TourApiPetTourItemDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

/**
 * TSK-DATA-009: {@code detailPetTour2} 호출 검증 (실제 응답 fixture 기반, 네트워크 없음).
 */
class TourApiClientPetTourDetailTest {

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
    @DisplayName("실제 성공 응답을 TourApiPetTourItemDto로 매핑하고, contentId를 쿼리로 전달한다")
    void fetchPetTourDetail_RealSuccess() throws IOException {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/detailPetTour2")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("contentId", "662268"))
                .andExpect(queryParam("_type", "json"))
                .andRespond(withSuccess(fixture("success-detail-pet-tour2-real-2026-11-19.json"), MediaType.APPLICATION_JSON));

        Optional<TourApiPetTourItemDto> result = client.fetchPetTourDetail("662268");

        mockServer.verify();
        assertThat(result).isPresent();
        TourApiPetTourItemDto item = result.get();
        assertThat(item.getContentId()).isEqualTo("662268");
        assertThat(item.getAccompanyType()).isEqualTo("일부구역 동반가능");
        assertThat(item.getAllowedPets()).isEqualTo("15kg 미만 중소형견만 입장 가능(맹견 및 대형견 입장 불가)");
        assertThat(item.getRequiredItems()).isEqualTo("반려동물 유모차 탑승,이동장(켄넬)사용");
        assertThat(item.getEtcInfo()).contains("내부 실내 시설 입장 불가");
    }

    @Test
    @DisplayName("정책 미등록 장소는 정상 응답의 items가 빈 문자열로 오며, 예외 없이 Optional.empty를 반환한다")
    void fetchPetTourDetail_RealEmpty() throws IOException {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/detailPetTour2")))
                .andRespond(withSuccess(fixture("success-detail-pet-tour2-empty-real-2026-11-19.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchPetTourDetail("2707444")).isEmpty();
    }

    @Test
    @DisplayName("루트에 resultCode/resultMsg만 있는 평평한 게이트웨이 오류도 실제 사유와 함께 PublicDataException으로 변환한다")
    void fetchPetTourDetail_FlatGatewayError() throws IOException {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/detailPetTour2")))
                .andRespond(withSuccess(fixture("error-flat-missing-content-id-real-2026-11-19.json"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchPetTourDetail("1"))
                .isInstanceOf(PublicDataException.class)
                .hasMessageContaining("11")
                .hasMessageContaining("NO_MANDATORY_REQUEST_PARAMETERS_ERROR");
    }

    @Test
    @DisplayName("평평한 오류가 4xx 상태로 와도 본문의 실제 사유를 노출한다")
    void fetchPetTourDetail_FlatGatewayErrorWith4xx() throws IOException {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/detailPetTour2")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fixture("error-flat-missing-content-id-real-2026-11-19.json")));

        assertThatThrownBy(() -> client.fetchPetTourDetail("1"))
                .isInstanceOf(PublicDataException.class)
                .hasMessageContaining("NO_MANDATORY_REQUEST_PARAMETERS_ERROR");
    }

    @Test
    @DisplayName("contentId가 비어 있으면 API를 호출하지 않고 IllegalArgumentException을 던진다")
    void fetchPetTourDetail_BlankContentId() {
        assertThatThrownBy(() -> client.fetchPetTourDetail(" "))
                .isInstanceOf(IllegalArgumentException.class);
        mockServer.verify(); // 호출 기대가 없으므로 어떤 요청도 나가지 않았어야 한다
    }

    private String fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/tourapi/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
