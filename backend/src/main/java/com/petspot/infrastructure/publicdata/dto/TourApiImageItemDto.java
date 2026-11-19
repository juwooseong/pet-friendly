package com.petspot.infrastructure.publicdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TourAPI {@code detailImage2}(이미지 정보 조회) 응답 item.
 * 실제 응답 fixture: {@code src/test/resources/tourapi/success-detail-image2-real-2026-11-19.json}
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiImageItemDto {

    @JsonProperty("contentid")
    private String contentId;

    /** 원본 이미지 URL */
    @JsonProperty("originimgurl")
    private String originalUrl;

    /** 작은 이미지 URL (관찰된 샘플에서는 원본과 같은 URL인 경우가 많다) */
    @JsonProperty("smallimageurl")
    private String smallUrl;

    /** 공공누리 유형 (Type1 출처표시, Type3 출처표시+변경금지 등) */
    @JsonProperty("cpyrhtDivCd")
    private String copyrightType;

    /** 이미지 순번 */
    @JsonProperty("serialnum")
    private String serialNumber;
}
