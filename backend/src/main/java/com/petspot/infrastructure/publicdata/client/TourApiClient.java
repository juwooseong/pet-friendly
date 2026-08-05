package com.petspot.infrastructure.publicdata.client;

import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 한국관광공사 TourAPI (KorPetTourService) HTTP 통신 클라이언트 (Spring Boot 3.2+ RestClient)
 */
@Slf4j
@Component
public class TourApiClient {

    private static final String SUCCESS_RESULT_CODE = "0000";

    private final RestClient restClient;

    @Value("${public-data.tour-api.service-key:test-service-key}")
    private String serviceKey;

    @Value("${public-data.tour-api.baseUrl:https://apis.data.go.kr/B551011/KorPetTourService}")
    private String baseUrl;

    @Value("${public-data.tour-api.mobile-os:ETC}")
    private String mobileOs;

    @Value("${public-data.tour-api.mobile-app:PetSpot}")
    private String mobileApp;

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
        URI targetUri = buildUri("/detailPetTour1", numOfRows, pageNo, contentTypeId);
        log.info("Requesting TourAPI via RestClient: {}", targetUri);

        try {
            TourApiResponseDto responseDto = restClient.get()
                    .uri(targetUri)
                    .retrieve()
                    .body(TourApiResponseDto.class);

            if (responseDto == null) {
                throw new PublicDataException("TourAPI 응답 본문이 null입니다.");
            }

            validateResponse(responseDto);
            return responseDto;

        } catch (PublicDataException e) {
            throw e;
        } catch (Exception e) {
            log.error("TourAPI 통신 중 오류 발생: URI={}", targetUri, e);
            throw new PublicDataException("TourAPI 통신 중 예외가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private URI buildUri(String path, int numOfRows, int pageNo, String contentTypeId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + path)
                .queryParam("serviceKey", serviceKey)
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json");

        if (contentTypeId != null && !contentTypeId.isBlank()) {
            builder.queryParam("contentTypeId", contentTypeId);
        }

        // ServiceKey 인코딩 중복 방지를 위한 build(true) 사용
        return builder.build(true).toUri();
    }

    private void validateResponse(TourApiResponseDto responseDto) {
        if (responseDto == null || responseDto.getResponse() == null || responseDto.getResponse().getHeader() == null) {
            throw new PublicDataException("TourAPI 응답 데이터 형식이 올바르지 않습니다 (Header Null).");
        }

        TourApiResponseDto.Header header = responseDto.getResponse().getHeader();
        if (!SUCCESS_RESULT_CODE.equals(header.getResultCode())) {
            log.warn("TourAPI Error Result: code={}, msg={}", header.getResultCode(), header.getResultMsg());
            throw new PublicDataException("TourAPI 오류 응답: [" + header.getResultCode() + "] " + header.getResultMsg());
        }
    }
}
