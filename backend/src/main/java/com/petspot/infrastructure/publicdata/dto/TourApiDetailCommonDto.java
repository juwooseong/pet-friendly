package com.petspot.infrastructure.publicdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TourAPI {@code detailCommon2}(공통 정보 조회) 응답 item 중 PetSpot이 쓰는 필드.
 * 실제 응답 fixture: {@code src/test/resources/tourapi/success-detail-common2-real-2026-09-14.json}
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiDetailCommonDto {

    @JsonProperty("contentid")
    private String contentId;

    /** 장소 유형 (12 관광지, 14 문화시설, 15 행사, 25 여행코스, 28 레포츠, 32 숙박, 38 쇼핑, 39 음식점) - detailIntro2 호출에 필요 */
    @JsonProperty("contenttypeid")
    private String contentTypeId;

    /** 장소 개요(소개 문장). HTML 태그(&lt;br&gt; 등)가 섞여 올 수 있다 */
    @JsonProperty("overview")
    private String overview;

    /** 홈페이지 (HTML 링크 태그로 오는 경우가 많다) */
    @JsonProperty("homepage")
    private String homepage;
}
