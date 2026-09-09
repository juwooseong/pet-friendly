package com.petspot.infrastructure.publicdata.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.infrastructure.publicdata.dto.TourApiErrorEnvelopeDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 한국관광공사 TourAPI (KorPetTourService2 - 반려동물 동반여행 서비스) HTTP 통신 클라이언트 (Spring Boot 3.2+ RestClient)
 *
 * <p>TSK-DATA-002 실제 호출 검증 결과: baseUrl은 B551011/KorPetTourService2 이며, 지원이 확인된
 * 오퍼레이션은 areaBasedList2 / locationBasedList2 / detailCommon2 / detailIntro2 / detailImage2 /
 * searchKeyword2 이다 (data.go.kr 게이트웨이가 reasonCode 30 "SERVICE_KEY_IS_NOT_REGISTERED_ERROR"로
 * 응답 - 즉 경로 자체는 인식됨). 반대로 detailPetTour1/2, areaBasedSyncList2, KorPetTourService(2 없는 구버전),
 * KorPetTourService1, KorService1 은 reasonCode 12 "NO_OPENAPI_SERVICE_ERROR"로 응답 - 즉 존재하지 않는 경로임.
 */
@Slf4j
@Component
public class TourApiClient {

    private static final String SUCCESS_RESULT_CODE = "0000";

    /** TSK-DATA-002 실제 호출로 확인된 목록 조회 오퍼레이션 (구 "detailPetTour1"은 존재하지 않는 경로였음) */
    private static final String LIST_OPERATION_PATH = "/areaBasedList2";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${public-data.tour-api.service-key:test-service-key}")
    private String serviceKey;

    @Value("${public-data.tour-api.baseUrl:https://apis.data.go.kr/B551011/KorPetTourService2}")
    private String baseUrl;

    @Value("${public-data.tour-api.mobile-os:ETC}")
    private String mobileOs;

    @Value("${public-data.tour-api.mobile-app:PetSpot}")
    private String mobileApp;

    @Autowired
    public TourApiClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    // 테스트 또는 커스텀 RestClient 생성을 위한 생성자 Overloading
    public TourApiClient(RestClient restClient, String serviceKey, String baseUrl) {
        this.restClient = restClient;
        this.serviceKey = serviceKey;
        this.baseUrl = baseUrl;
        this.mobileOs = "ETC";
        this.mobileApp = "PetSpot";
    }

    /**
     * 반려동물 동반 여행 정보 위치 기반 / 키워드 / 목록 조회
     *
     * @param numOfRows 한 페이지 결과 수
     * @param pageNo   페이지 번호
     * @return TourApiResponseDto 파싱 완료된 DTO
     */
    public TourApiResponseDto fetchPetPlaces(int numOfRows, int pageNo) {
        return fetchPetPlaces(numOfRows, pageNo, null);
    }

    /**
     * 반려동물 동반 여행 정보 지역/분류별 목록 조회 (contentTypeId 필터 추가 지원)
     */
    public TourApiResponseDto fetchPetPlaces(int numOfRows, int pageNo, String contentTypeId) {
        URI targetUri = buildUri(LIST_OPERATION_PATH, numOfRows, pageNo, contentTypeId);
        log.info("Requesting TourAPI via RestClient: {}", targetUri);

        String rawBody;
        try {
            rawBody = restClient.get()
                    .uri(targetUri)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            // 4xx/5xx 상태 코드라도 data.go.kr 게이트웨이는 본문에 실제 오류 코드(OpenAPI_ServiceResponse)를
            // 담아 보내는 경우가 많으므로, 본문이 있으면 버리지 않고 동일한 파싱 경로로 넘겨 실제 사유를 노출한다.
            String errorBody = e.getResponseBodyAsString();
            if (errorBody != null && !errorBody.isBlank()) {
                log.warn("TourAPI 오류 상태 응답 수신: status={}, URI={}", e.getStatusCode(), targetUri);
                validateResponse(parseSuccessBody(errorBody), errorBody);
                // validateResponse가 항상 예외를 던지므로 이 지점에는 도달하지 않는다.
                throw new PublicDataException("TourAPI 오류 응답을 처리하지 못했습니다: " + e.getStatusCode());
            }
            log.error("TourAPI 통신 중 오류 발생(본문 없음): URI={}", targetUri, e);
            throw new PublicDataException("TourAPI 통신 중 예외가 발생했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("TourAPI 통신 중 오류 발생: URI={}", targetUri, e);
            throw new PublicDataException("TourAPI 통신 중 예외가 발생했습니다: " + e.getMessage(), e);
        }

        if (rawBody == null || rawBody.isBlank()) {
            throw new PublicDataException("TourAPI 응답 본문이 null이거나 비어 있습니다.");
        }

        TourApiResponseDto responseDto = parseSuccessBody(rawBody);
        validateResponse(responseDto, rawBody);
        return responseDto;
    }

    private URI buildUri(String path, int numOfRows, int pageNo, String contentTypeId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + path)
                .queryParam("serviceKey", serviceKey)
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("arrange", "A")
                .queryParam("_type", "json");

        if (contentTypeId != null && !contentTypeId.isBlank()) {
            builder.queryParam("contentTypeId", contentTypeId);
        }

        // ServiceKey 인코딩 중복 방지를 위한 build(true) 사용
        return builder.build(true).toUri();
    }

    /**
     * 정상 응답 스키마({@code response.header/body})로 우선 파싱을 시도한다.
     * JSON 파싱 자체가 실패하면(XML 오류 응답 등 포함) PublicDataException으로 변환한다.
     */
    private TourApiResponseDto parseSuccessBody(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, TourApiResponseDto.class);
        } catch (Exception e) {
            // JSON이 아닌 응답(XML 오류 등)일 수 있으므로 원문 일부를 남기고 게이트웨이 오류 처리로 넘긴다.
            log.warn("TourAPI 응답을 표준 스키마로 파싱하지 못했습니다. 게이트웨이 오류 여부를 확인합니다.");
            return null;
        }
    }

    /**
     * data.go.kr 게이트웨이는 정상 응답과 별개로 완전히 다른 루트 키({@code OpenAPI_ServiceResponse})를
     * 갖는 오류 응답을 반환할 수 있다 (서비스키 미등록, 존재하지 않는 오퍼레이션 등).
     * 이 경우 표준 DTO만으로는 header가 항상 null이 되므로, 실제 오류 코드/메시지를 추출해 예외로 노출한다.
     */
    private void validateResponse(TourApiResponseDto responseDto, String rawBody) {
        if (responseDto == null || responseDto.getResponse() == null || responseDto.getResponse().getHeader() == null) {
            TourApiErrorEnvelopeDto errorEnvelope = tryParseErrorEnvelope(rawBody);
            if (errorEnvelope != null && errorEnvelope.isPresent()) {
                TourApiErrorEnvelopeDto.CmmMsgHeader header = errorEnvelope.getOpenApiServiceResponse().getCmmMsgHeader();
                log.warn("TourAPI Gateway Error: reasonCode={}, errMsg={}, authMsg={}",
                        header.getReturnReasonCode(), header.getErrMsg(), header.getReturnAuthMsg());
                throw new PublicDataException("TourAPI 게이트웨이 오류: [" + header.getReturnReasonCode() + "] "
                        + header.getErrMsg() + " (" + header.getReturnAuthMsg() + ")");
            }
            throw new PublicDataException("TourAPI 응답 데이터 형식이 올바르지 않습니다 (Header Null).");
        }

        TourApiResponseDto.Header header = responseDto.getResponse().getHeader();
        if (!SUCCESS_RESULT_CODE.equals(header.getResultCode())) {
            log.warn("TourAPI Error Result: code={}, msg={}", header.getResultCode(), header.getResultMsg());
            throw new PublicDataException("TourAPI 오류 응답: [" + header.getResultCode() + "] " + header.getResultMsg());
        }
    }

    private TourApiErrorEnvelopeDto tryParseErrorEnvelope(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, TourApiErrorEnvelopeDto.class);
        } catch (Exception e) {
            return null;
        }
    }
}
