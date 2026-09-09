package com.petspot.infrastructure.publicdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TourAPI(data.go.kr 공통 게이트웨이) 인증/게이트웨이 레벨 오류 응답 DTO.
 *
 * <p>data.go.kr 게이트웨이는 정상 응답({@link TourApiResponseDto}, 루트 키 "response")과
 * 별개로, 서비스키 미등록/오퍼레이션 미존재 등 게이트웨이 레벨 오류 시
 * 루트 키가 "OpenAPI_ServiceResponse"인 전혀 다른 스키마로 응답한다.
 * (TSK-DATA-002 실제 호출로 확인된 형태, 예: SERVICE_KEY_IS_NOT_REGISTERED_ERROR / NO_OPENAPI_SERVICE_ERROR)
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiErrorEnvelopeDto {

    @JsonProperty("OpenAPI_ServiceResponse")
    private ServiceResponse openApiServiceResponse;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServiceResponse {
        @JsonProperty("cmmMsgHeader")
        private CmmMsgHeader cmmMsgHeader;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CmmMsgHeader {
        @JsonProperty("errMsg")
        private String errMsg;

        @JsonProperty("returnAuthMsg")
        private String returnAuthMsg;

        @JsonProperty("returnReasonCode")
        private String returnReasonCode;
    }

    public boolean isPresent() {
        return openApiServiceResponse != null && openApiServiceResponse.getCmmMsgHeader() != null;
    }
}
