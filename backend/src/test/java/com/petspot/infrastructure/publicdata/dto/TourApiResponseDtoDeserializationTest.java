package com.petspot.infrastructure.publicdata.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 오프라인 DTO 역직렬화 테스트 (TSK-DATA-002).
 * src/test/resources/tourapi/*.json 고정 fixture를 사용하며 네트워크/실제 키 없이 동작한다.
 */
class TourApiResponseDtoDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("표준 성공 스키마(response.header/body.items.item) 및 mapx/mapy 문자열 좌표 파싱 검증")
    void deserialize_successSample() throws IOException {
        TourApiResponseDto dto = readFixture("success-area-based-list2-sample.json", TourApiResponseDto.class);

        assertThat(dto.getResponse()).isNotNull();
        assertThat(dto.getResponse().getHeader().getResultCode()).isEqualTo("0000");

        List<TourApiPlaceItemDto> items = dto.getResponse().getBody().getItemList();
        assertThat(items).hasSize(3);

        TourApiPlaceItemDto first = items.get(0);
        assertThat(first.getContentId()).isEqualTo("126216");
        assertThat(first.getTitle()).isEqualTo("경복궁");
        assertThat(first.getFullAddress()).isEqualTo("서울특별시 종로구 사직로 161");
        // mapx/mapy가 JSON 문자열("126.9769112092")로 와도 안전하게 Double로 파싱된다.
        assertThat(first.getLongitude()).isEqualTo(126.9769112092);
        assertThat(first.getLatitude()).isEqualTo(37.5788439714);
        assertThat(first.getAccompanyTypeCd()).isEqualTo("PET002");
        assertThat(first.getPetWeightLimitText()).isEqualTo("10kg 이하 소형견 권장");
    }

    @Test
    @DisplayName("mapx/mapy가 빈 문자열인 항목은 예외 없이 null 좌표로 파싱되어야 한다 (전체 페이지 파싱 실패 방지)")
    void deserialize_blankCoordinates_doesNotThrow() throws IOException {
        TourApiResponseDto dto = readFixture("success-area-based-list2-sample.json", TourApiResponseDto.class);

        TourApiPlaceItemDto itemWithBlankCoords = dto.getResponse().getBody().getItemList().get(2);
        assertThat(itemWithBlankCoords.getLongitude()).isNull();
        assertThat(itemWithBlankCoords.getLatitude()).isNull();
    }

    @Test
    @DisplayName("게이트웨이 오류 응답(OpenAPI_ServiceResponse) 스키마는 표준 DTO에서 response가 null로 파싱된다")
    void deserialize_errorEnvelope_asStandardDto_hasNullResponse() throws IOException {
        TourApiResponseDto dto = readFixture("error-service-key-not-registered.json", TourApiResponseDto.class);
        assertThat(dto.getResponse()).isNull();
    }

    @Test
    @DisplayName("게이트웨이 오류 응답 전용 DTO로는 실제 오류 코드/메시지가 추출된다 (실제 호출로 확보한 응답)")
    void deserialize_errorEnvelope_withDedicatedDto() throws IOException {
        TourApiErrorEnvelopeDto errorDto = readFixture("error-service-key-not-registered.json", TourApiErrorEnvelopeDto.class);
        assertThat(errorDto.isPresent()).isTrue();
        assertThat(errorDto.getOpenApiServiceResponse().getCmmMsgHeader().getReturnReasonCode()).isEqualTo("30");
        assertThat(errorDto.getOpenApiServiceResponse().getCmmMsgHeader().getErrMsg()).isEqualTo("SERVICE_KEY_IS_NOT_REGISTERED_ERROR");

        TourApiErrorEnvelopeDto noServiceDto = readFixture("error-no-openapi-service.json", TourApiErrorEnvelopeDto.class);
        assertThat(noServiceDto.getOpenApiServiceResponse().getCmmMsgHeader().getReturnReasonCode()).isEqualTo("12");
        assertThat(noServiceDto.getOpenApiServiceResponse().getCmmMsgHeader().getErrMsg()).isEqualTo("NO_OPENAPI_SERVICE_ERROR");
    }

    private <T> T readFixture(String fileName, Class<T> type) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("tourapi/" + fileName)) {
            assertThat(is).as("fixture file must exist: " + fileName).isNotNull();
            return objectMapper.readValue(is, type);
        }
    }
}
