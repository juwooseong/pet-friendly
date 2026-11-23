package com.petspot.infrastructure.publicdata.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.infrastructure.publicdata.dto.TourApiDetailCommonDto;
import com.petspot.infrastructure.publicdata.dto.TourApiErrorEnvelopeDto;
import com.petspot.infrastructure.publicdata.dto.TourApiImageItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiPetTourItemDto;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 한국관광공사 TourAPI (KorPetTourService2 - 반려동물 동반여행 서비스) HTTP 통신 클라이언트 (Spring Boot 3.2+ RestClient)
 *
 * <p>baseUrl은 {@code B551011/KorPetTourService2}이다. 실제 호출로 사용을 확인한 오퍼레이션:
 * <ul>
 *   <li>{@code areaBasedList2} - 장소 기본정보 목록 (좌표/주소/이미지/분류)</li>
 *   <li>{@code detailPetTour2} - 장소별 반려동물 동반 정책 (동반 가능 동물·필요 사항 등). {@code contentId} 필수</li>
 *   <li>{@code detailCommon2} / {@code detailIntro2} / {@code detailInfo2} - 상세 공통/소개/반복 정보 (TSK-DATA-005)</li>
 * </ul>
 *
 * <p><b>정정 (ADR-009, 2026-11-19):</b> TSK-DATA-002(2026-09-09)는 {@code detailPetTour2}를 "존재하지 않는 경로"로
 * 기록했으나, 이는 서비스키 승인 전 상태에서의 오판이었다. 승인된 키로 호출하면 정상 응답(resultCode 0000)과
 * 반려동물 정책 데이터를 반환한다.
 *
 * <p>data.go.kr 게이트웨이 오류 응답은 세 가지 형태가 관찰되었고 모두 {@link PublicDataException}으로 변환한다:
 * 표준 봉투의 non-0000 resultCode, {@code OpenAPI_ServiceResponse} 봉투(서비스키 미등록 등),
 * 루트에 {@code resultCode}/{@code resultMsg}만 있는 평평한 봉투(필수 파라미터 누락 등).
 */
@Slf4j
@Component
public class TourApiClient {

    private static final String SUCCESS_RESULT_CODE = "0000";

    /** 장소 기본정보 목록 조회 오퍼레이션 */
    private static final String LIST_OPERATION_PATH = "/areaBasedList2";

    /**
     * 장소 동기화 목록 오퍼레이션 (TSK-DATA-012). areaBasedList2와 같은 필드 + {@code showflag}(1 노출/0 숨김).
     * {@code modifiedtime}은 수정 시각 <b>앞자리 일치</b> 조건이다(실제 확인 2026-11-23: "202611" → 11월 수정 577건,
     * "20261101" → 그날 수정분). 조건 없이 부르면 숨긴 장소까지 전체(10,095건 = 노출 9,632 + 숨김 463)를 준다.
     */
    private static final String SYNC_LIST_OPERATION_PATH = "/petTourSyncList2";

    /** 장소별 반려동물 동반 정책 조회 오퍼레이션 (contentId 필수, 목록 조회 불가) */
    private static final String PET_TOUR_DETAIL_OPERATION_PATH = "/detailPetTour2";

    /** 장소 공통 정보(개요/홈페이지/장소 유형) 조회 오퍼레이션 (contentId 필수) */
    private static final String DETAIL_COMMON_OPERATION_PATH = "/detailCommon2";

    /** 장소 소개 정보(운영시간/쉬는 날 등) 조회 오퍼레이션 (contentId, contentTypeId 필수) */
    private static final String DETAIL_INTRO_OPERATION_PATH = "/detailIntro2";

    /** 장소 사진 목록 조회 오퍼레이션 (contentId, imageYN 필요) */
    private static final String DETAIL_IMAGE_OPERATION_PATH = "/detailImage2";

    private final RestClient restClient;
    // 결과가 없으면 TourAPI는 items를 객체 대신 빈 문자열("")로 보낸다 - 목록 DTO에서는 null(빈 목록)로 받는다
    // (2026-11-23 실제 관찰: petTourSyncList2 modifiedtime=20261122, TSK-DATA-012)
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

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
        URI targetUri = buildListUri(numOfRows, pageNo, contentTypeId);
        log.info("Requesting TourAPI via RestClient: {}", maskServiceKey(targetUri));

        String rawBody = fetchRaw(targetUri);
        TourApiResponseDto responseDto = parseSuccessBody(rawBody);
        validateResponse(responseDto, rawBody);
        return responseDto;
    }

    /**
     * 수정된 장소 목록을 조회한다 ({@code petTourSyncList2}, TSK-DATA-012).
     *
     * @param modifiedTimePrefix 수정 시각 앞자리(yyyyMMdd 또는 yyyyMM). null이면 전체(숨긴 장소 포함)
     */
    public TourApiResponseDto fetchPetPlaceChanges(int numOfRows, int pageNo, String modifiedTimePrefix) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + SYNC_LIST_OPERATION_PATH)
                .queryParam("serviceKey", encodedServiceKey())
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json");
        if (modifiedTimePrefix != null && !modifiedTimePrefix.isBlank()) {
            builder.queryParam("modifiedtime", modifiedTimePrefix);
        }
        URI targetUri = builder.build(true).toUri();
        log.info("Requesting TourAPI via RestClient: {}", maskServiceKey(targetUri));

        String rawBody = fetchRaw(targetUri);
        TourApiResponseDto responseDto = parseSuccessBody(rawBody);
        validateResponse(responseDto, rawBody);
        return responseDto;
    }

    /**
     * 장소 1곳의 반려동물 동반 정책을 조회한다 ({@code detailPetTour2}).
     *
     * <p>정책 정보가 등록되지 않은 장소는 정상 응답이지만 {@code items}가 빈 문자열({@code ""})로 내려오므로
     * {@link Optional#empty()}를 반환한다(실제 관찰: 숙박 contentId 2707444).
     *
     * @param contentId TourAPI 장소 ID (필수)
     * @return 반려동물 정책 item, 등록된 정책이 없으면 empty
     * @throws IllegalArgumentException contentId가 비어 있는 경우
     * @throws PublicDataException      통신 실패 또는 게이트웨이/API 오류 응답
     */
    public Optional<TourApiPetTourItemDto> fetchPetTourDetail(String contentId) {
        return fetchSingleItem(PET_TOUR_DETAIL_OPERATION_PATH, contentId, null)
                .map(item -> toDto(item, TourApiPetTourItemDto.class, contentId));
    }

    /**
     * 장소 공통 정보를 조회한다 ({@code detailCommon2}). 장소 개요({@code overview}), 홈페이지, 장소 유형
     * ({@code contenttypeid} - {@link #fetchDetailIntro} 호출에 필요)를 담고 있다.
     */
    public Optional<TourApiDetailCommonDto> fetchDetailCommon(String contentId) {
        return fetchSingleItem(DETAIL_COMMON_OPERATION_PATH, contentId, null)
                .map(item -> toDto(item, TourApiDetailCommonDto.class, contentId));
    }

    /**
     * 장소 소개 정보를 조회한다 ({@code detailIntro2}, {@code contentTypeId} 필수).
     * 필드 구성이 장소 유형마다 달라(관광지 usetime, 쇼핑 opentime, 음식점 opentimefood, 숙박 checkintime 등)
     * 텍스트 필드 전체를 키-값 맵으로 돌려준다. 해석은 {@code TourApiOperatingHoursExtractor}가 담당한다.
     */
    public Optional<Map<String, String>> fetchDetailIntro(String contentId, String contentTypeId) {
        if (contentTypeId == null || contentTypeId.isBlank()) {
            throw new IllegalArgumentException("contentTypeId는 필수입니다.");
        }
        return fetchSingleItem(DETAIL_INTRO_OPERATION_PATH, contentId, contentTypeId)
                .map(item -> {
                    Map<String, String> fields = new LinkedHashMap<>();
                    item.fields().forEachRemaining(e -> {
                        if (e.getValue().isValueNode()) {
                            fields.put(e.getKey(), e.getValue().asText());
                        }
                    });
                    return fields;
                });
    }

    /**
     * 장소 사진 목록을 조회한다 ({@code detailImage2}, imageYN=Y: 장소 사진 / N이면 음식 메뉴 사진).
     * 실제 응답(2026-11-19, contentId 1019041): 사진 4장, 각 항목에 originimgurl / smallimageurl / cpyrhtDivCd(Type1).
     *
     * @return 사진 목록 (없으면 빈 목록)
     */
    public List<TourApiImageItemDto> fetchDetailImages(String contentId) {
        List<TourApiImageItemDto> images = new ArrayList<>();
        for (JsonNode item : fetchItems(DETAIL_IMAGE_OPERATION_PATH, contentId, null, Map.of("imageYN", "Y"))) {
            images.add(toDto(item, TourApiImageItemDto.class, contentId));
        }
        return images;
    }

    /**
     * 단건 상세 오퍼레이션(detailCommon2 / detailIntro2 / detailPetTour2) 호출. 첫 item만 쓴다.
     * 데이터가 없는 장소는 정상 응답이지만 {@code items}가 빈 문자열이라 {@link Optional#empty()}를 반환한다
     * (실제 관찰: detailPetTour2 숙박 contentId 2707444).
     */
    private Optional<JsonNode> fetchSingleItem(String operationPath, String contentId, String contentTypeId) {
        List<JsonNode> items = fetchItems(operationPath, contentId, contentTypeId, Map.of());
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    /**
     * contentId 기반 상세 오퍼레이션 공통 호출. 응답 봉투를 검사하고 item 목록을 돌려준다.
     *
     * <p>TourAPI는 item이 1건이면 객체, 여러 건이면 배열, 0건이면 {@code items: ""}(문자열)로 보낸다 - 세 경우를 모두
     * 목록으로 통일한다. 게이트웨이 오류(평평한 resultCode / OpenAPI_ServiceResponse)는 {@link #gatewayError}로 변환한다.
     *
     * @param extraParams 오퍼레이션별 추가 쿼리 (예: detailImage2의 imageYN)
     * @throws IllegalArgumentException contentId가 비어 있는 경우
     * @throws PublicDataException      통신 실패 또는 게이트웨이/API 오류 응답
     */
    private List<JsonNode> fetchItems(String operationPath, String contentId, String contentTypeId,
                                      Map<String, String> extraParams) {
        if (contentId == null || contentId.isBlank()) {
            throw new IllegalArgumentException("contentId는 필수입니다.");
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + operationPath)
                .queryParam("serviceKey", encodedServiceKey())
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("contentId", contentId);
        if (contentTypeId != null) {
            builder.queryParam("contentTypeId", contentTypeId);
        }
        extraParams.forEach(builder::queryParam);
        // build(true)는 이미 인코딩된 값을 가정한다 - 서비스키는 encodedServiceKey()로 항상 인코딩해 넘긴다
        URI targetUri = builder.build(true).toUri();
        log.debug("Requesting TourAPI {}: contentId={}", operationPath, contentId);

        String rawBody = fetchRaw(targetUri);
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw gatewayError(rawBody);
        }

        JsonNode header = root.path("response").path("header");
        if (header.isMissingNode()) {
            throw gatewayError(rawBody);
        }
        String resultCode = header.path("resultCode").asText();
        if (!SUCCESS_RESULT_CODE.equals(resultCode)) {
            throw new PublicDataException("TourAPI 오류 응답: [" + resultCode + "] " + header.path("resultMsg").asText());
        }

        // 데이터가 없으면 items가 "" (문자열)로 온다. item은 1건이면 객체, 여러 건이면 배열로 온다.
        JsonNode items = root.path("response").path("body").path("items");
        if (!items.isObject()) {
            return List.of();
        }
        JsonNode item = items.path("item");
        List<JsonNode> result = new ArrayList<>();
        if (item.isArray()) {
            item.forEach(node -> {
                if (node.isObject()) {
                    result.add(node);
                }
            });
        } else if (item.isObject()) {
            result.add(item);
        }
        return result;
    }

    /** item JSON을 DTO로 변환한다. 형식이 예상과 다르면 어느 장소에서 실패했는지 남기고 PublicDataException으로 바꾼다. */
    private <T> T toDto(JsonNode item, Class<T> type, String contentId) {
        try {
            return objectMapper.treeToValue(item, type);
        } catch (Exception e) {
            throw new PublicDataException("TourAPI 응답 item을 해석하지 못했습니다: contentId=" + contentId, e);
        }
    }

    /**
     * HTTP GET 후 본문을 반환한다. 4xx/5xx라도 본문이 있으면 data.go.kr 게이트웨이의 실제 오류 사유를 담고 있는
     * 경우가 많으므로, 본문을 버리지 않고 오류 사유를 추출해 예외로 노출한다.
     */
    private String fetchRaw(URI targetUri) {
        String rawBody;
        try {
            rawBody = restClient.get()
                    .uri(targetUri)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            if (errorBody != null && !errorBody.isBlank()) {
                log.warn("TourAPI 오류 상태 응답 수신: status={}, URI={}", e.getStatusCode(), maskServiceKey(targetUri));
                throw gatewayError(errorBody);
            }
            log.error("TourAPI 통신 중 오류 발생(본문 없음): URI={}, error={}", maskServiceKey(targetUri), maskSecret(e.getMessage()));
            throw new PublicDataException("TourAPI 통신 중 예외가 발생했습니다: " + maskSecret(e.getMessage()));
        } catch (Exception e) {
            // 원인 예외를 그대로 연결하면 스택트레이스/메시지에 서비스키가 섞일 수 있어 가린 메시지만 남긴다
            log.error("TourAPI 통신 중 오류 발생: URI={}, error={}", maskServiceKey(targetUri), maskSecret(e.getMessage()));
            throw new PublicDataException("TourAPI 통신 중 예외가 발생했습니다: " + maskSecret(e.getMessage()));
        }

        if (rawBody == null || rawBody.isBlank()) {
            throw new PublicDataException("TourAPI 응답 본문이 null이거나 비어 있습니다.");
        }
        return rawBody;
    }

    private URI buildListUri(int numOfRows, int pageNo, String contentTypeId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + LIST_OPERATION_PATH)
                .queryParam("serviceKey", encodedServiceKey())
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("arrange", "A")
                .queryParam("_type", "json");

        if (contentTypeId != null && !contentTypeId.isBlank()) {
            builder.queryParam("contentTypeId", contentTypeId);
        }

        // build(true)는 쿼리 값이 이미 인코딩되었다고 가정한다 - 서비스키는 encodedServiceKey()로 인코딩해 넘긴다
        return builder.build(true).toUri();
    }

    /**
     * data.go.kr는 같은 키를 "Encoding"(%2B 등 인코딩됨)과 "Decoding"(+, /, = 포함 원문) 두 형태로 보여준다.
     * 어느 쪽을 설정해도 동작하도록, 이미 인코딩된 키(% 포함)는 그대로 쓰고 원문 키는 URL 인코딩한다.
     * (2026-11-19: Decoding 키 설정 시 "Invalid character '=' for QUERY_PARAM"으로 요청 전에 실패하던 문제 수정)
     */
    String encodedServiceKey() {
        if (serviceKey == null) {
            return "";
        }
        return serviceKey.contains("%") ? serviceKey : URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
    }

    /** 예외/로그 메시지에 서비스키(원문·인코딩 형태 모두)가 섞여 있으면 가린다. */
    private String maskSecret(String message) {
        if (message == null || serviceKey == null || serviceKey.isBlank()) {
            return message;
        }
        String masked = message.replace(serviceKey, "***");
        String encoded = encodedServiceKey();
        return encoded.isBlank() ? masked : masked.replace(encoded, "***");
    }

    /** 로그에 서비스키가 남지 않도록 쿼리의 serviceKey 값을 가린다. */
    private String maskServiceKey(URI uri) {
        return uri.toString().replaceAll("serviceKey=[^&]*", "serviceKey=***");
    }

    /**
     * 정상 응답 스키마({@code response.header/body})로 우선 파싱을 시도한다.
     * JSON 파싱 자체가 실패하면(XML 오류 응답 등 포함) null을 반환해 게이트웨이 오류 처리로 넘긴다.
     */
    private TourApiResponseDto parseSuccessBody(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, TourApiResponseDto.class);
        } catch (Exception e) {
            log.warn("TourAPI 응답을 표준 스키마로 파싱하지 못했습니다. 게이트웨이 오류 여부를 확인합니다.");
            return null;
        }
    }

    private void validateResponse(TourApiResponseDto responseDto, String rawBody) {
        if (responseDto == null || responseDto.getResponse() == null || responseDto.getResponse().getHeader() == null) {
            throw gatewayError(rawBody);
        }

        TourApiResponseDto.Header header = responseDto.getResponse().getHeader();
        if (!SUCCESS_RESULT_CODE.equals(header.getResultCode())) {
            log.warn("TourAPI Error Result: code={}, msg={}", header.getResultCode(), header.getResultMsg());
            throw new PublicDataException("TourAPI 오류 응답: [" + header.getResultCode() + "] " + header.getResultMsg());
        }
    }

    /**
     * 표준 봉투가 아닌 오류 응답에서 실제 사유를 추출한다.
     * <ol>
     *   <li>{@code OpenAPI_ServiceResponse.cmmMsgHeader} (서비스키 미등록, 존재하지 않는 서비스 등)</li>
     *   <li>루트의 {@code resultCode}/{@code resultMsg} (예: {@code 11 NO_MANDATORY_REQUEST_PARAMETERS_ERROR1(contentId)},
     *       {@code 10 INVALID_REQUEST_PARAMETER_ERROR(contentId)} - 2026-11-19 실제 관찰)</li>
     *   <li>표준 봉투의 non-0000 header</li>
     * </ol>
     */
    private PublicDataException gatewayError(String rawBody) {
        TourApiErrorEnvelopeDto errorEnvelope = tryParseErrorEnvelope(rawBody);
        if (errorEnvelope != null && errorEnvelope.isPresent()) {
            TourApiErrorEnvelopeDto.CmmMsgHeader header = errorEnvelope.getOpenApiServiceResponse().getCmmMsgHeader();
            log.warn("TourAPI Gateway Error: reasonCode={}, errMsg={}, authMsg={}",
                    header.getReturnReasonCode(), header.getErrMsg(), header.getReturnAuthMsg());
            return new PublicDataException("TourAPI 게이트웨이 오류: [" + header.getReturnReasonCode() + "] "
                    + header.getErrMsg() + " (" + header.getReturnAuthMsg() + ")");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (root.hasNonNull("resultCode")) {
                String code = root.path("resultCode").asText();
                String msg = root.path("resultMsg").asText();
                log.warn("TourAPI Gateway Error (flat): resultCode={}, resultMsg={}", code, msg);
                return new PublicDataException("TourAPI 게이트웨이 오류: [" + code + "] " + msg);
            }
            JsonNode header = root.path("response").path("header");
            if (header.hasNonNull("resultCode") && !SUCCESS_RESULT_CODE.equals(header.path("resultCode").asText())) {
                return new PublicDataException("TourAPI 오류 응답: [" + header.path("resultCode").asText() + "] "
                        + header.path("resultMsg").asText());
            }
        } catch (Exception ignored) {
            // JSON이 아닌 응답(XML 등) - 아래 일반 오류로 처리
        }
        return new PublicDataException("TourAPI 응답 데이터 형식이 올바르지 않습니다 (Header Null).");
    }

    private TourApiErrorEnvelopeDto tryParseErrorEnvelope(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, TourApiErrorEnvelopeDto.class);
        } catch (Exception e) {
            return null;
        }
    }
}
