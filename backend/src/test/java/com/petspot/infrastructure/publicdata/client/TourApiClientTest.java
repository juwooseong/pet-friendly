package com.petspot.infrastructure.publicdata.client;

import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import com.petspot.infrastructure.publicdata.service.PublicDataIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class TourApiClientTest {

    private MockRestServiceServer mockServer;
    private TourApiClient tourApiClient;
    private PublicDataIngestionService publicDataIngestionService;

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorPetTourService";
    private static final String SERVICE_KEY = "test-service-key";

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        tourApiClient = new TourApiClient(restClient, SERVICE_KEY, BASE_URL);
        publicDataIngestionService = new PublicDataIngestionService(tourApiClient);
    }

    @Test
    @DisplayName("TourAPI 정상 응답 JSON이 TourApiResponseDto 및 PlaceItemDto로 올바르게 매핑되는지 검증")
    void fetchPetPlaces_Success() {
        // given
        String mockJsonResponse = """
            {
              "response": {
                "header": {
                  "resultCode": "0000",
                  "resultMsg": "OK"
                },
                "body": {
                  "items": {
                    "item": [
                      {
                        "contentid": "123456",
                        "contenttypeid": "39",
                        "title": "댕댕이 힐링 카페",
                        "addr1": "서울특별시 마포구 월드컵북로 100",
                        "addr2": "1층",
                        "tel": "02-1234-5678",
                        "mapx": 126.901234,
                        "mapy": 37.556789,
                        "firstimage": "https://example.com/image.jpg",
                        "acmpyPsblCpam": "10kg 이하 소형견 가능",
                        "claAcmpyInfo": "실내 동반 시 리드줄 착용 필수"
                      }
                    ]
                  },
                  "numOfRows": 10,
                  "pageNo": 1,
                  "totalCount": 1
                }
              }
            }
            """;

        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        // when
        int numOfRows = 10;
        int pageNo = 1;
        System.out.println("==================================================");
        System.out.println("[TEST RUN] fetchPetPlaces_Success");
        System.out.println("[CALL PARAMS] numOfRows: " + numOfRows + ", pageNo: " + pageNo);
        
        TourApiResponseDto responseDto = tourApiClient.fetchPetPlaces(numOfRows, pageNo);

        // then
        mockServer.verify();
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getResponse().getHeader().getResultCode()).isEqualTo("0000");

        List<TourApiPlaceItemDto> items = responseDto.getResponse().getBody().getItemList();
        assertThat(items).hasSize(1);

        TourApiPlaceItemDto item = items.get(0);
        System.out.println("[RESULT OBJECT] Header ResultCode: " + responseDto.getResponse().getHeader().getResultCode());
        System.out.println("[RESULT ITEM] ContentID: " + item.getContentId());
        System.out.println("[RESULT ITEM] Title: " + item.getTitle());
        System.out.println("[RESULT ITEM] FullAddress: " + item.getFullAddress());
        System.out.println("[RESULT ITEM] Coordinates: (" + item.getLatitude() + ", " + item.getLongitude() + ")");
        System.out.println("[RESULT ITEM] Pet Policy Text: " + item.getPetWeightLimitText());
        System.out.println("==================================================");

        assertThat(item.getContentId()).isEqualTo("123456");
        assertThat(item.getTitle()).isEqualTo("댕댕이 힐링 카페");
        assertThat(item.getFullAddress()).isEqualTo("서울특별시 마포구 월드컵북로 100 1층");
        assertThat(item.getLongitude()).isEqualTo(126.901234);
        assertThat(item.getLatitude()).isEqualTo(37.556789);
        assertThat(item.getPetWeightLimitText()).isEqualTo("10kg 이하 소형견 가능");
    }

    @Test
    @DisplayName("PublicDataIngestionService를 통한 파싱 및 항목 추출 검증 (TSK-BE-005 사용성)")
    void service_fetchPetPlaces_Success() {
        // given
        String mockJsonResponse = """
            {
              "response": {
                "header": {
                  "resultCode": "0000",
                  "resultMsg": "OK"
                },
                "body": {
                  "items": {
                    "item": [
                      {
                        "contentid": "999999",
                        "title": "애견 동반 파크",
                        "addr1": "경기도 용인시 기흥구",
                        "mapx": 127.123,
                        "mapy": 37.123
                      }
                    ]
                  },
                  "numOfRows": 1,
                  "pageNo": 1,
                  "totalCount": 100
                }
              }
            }
            """;

        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        // when
        int numOfRows = 1;
        int pageNo = 1;
        System.out.println("==================================================");
        System.out.println("[TEST RUN] service_fetchPetPlaces_Success");
        System.out.println("[CALL PARAMS] numOfRows: " + numOfRows + ", pageNo: " + pageNo);

        List<TourApiPlaceItemDto> items = publicDataIngestionService.fetchPetPlaces(numOfRows, pageNo);

        // then
        assertThat(items).hasSize(1);
        System.out.println("[RESULT ITEM] Title: " + items.get(0).getTitle());
        System.out.println("[RESULT ITEM] Address: " + items.get(0).getFullAddress());
        System.out.println("==================================================");
        assertThat(items.get(0).getTitle()).isEqualTo("애견 동반 파크");
    }

    @Test
    @DisplayName("TourAPI 오류 응답(resultCode != 0000) 수신 시 PublicDataException 예외 발생 검증")
    void fetchPetPlaces_ApiErrorResult_ThrowsException() {
        // given
        String mockErrorJsonResponse = """
            {
              "response": {
                "header": {
                  "resultCode": "20",
                  "resultMsg": "SERVICE_ACCESS_DENIED_ERROR"
                }
              }
            }
            """;

        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockErrorJsonResponse, MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> tourApiClient.fetchPetPlaces(10, 1))
                .isInstanceOf(PublicDataException.class)
                .hasMessageContaining("SERVICE_ACCESS_DENIED_ERROR");
    }

    @Test
    @DisplayName("서버 HTTP 500 에러 발생 시 PublicDataException으로 변환 예외 처리 검증")
    void fetchPetPlaces_HttpServerError_ThrowsException() {
        // given
        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> tourApiClient.fetchPetPlaces(10, 1))
                .isInstanceOf(PublicDataException.class)
                .hasMessageContaining("500 Internal Server Error");
    }
}
