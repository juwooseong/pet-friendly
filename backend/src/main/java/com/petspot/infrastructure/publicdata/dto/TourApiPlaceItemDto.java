package com.petspot.infrastructure.publicdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한국관광공사 TourAPI 반려동물 동반 장소 항목 개별 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiPlaceItemDto {

    @JsonProperty("contentid")
    private String contentId;

    @JsonProperty("contenttypeid")
    private String contentTypeId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("addr1")
    private String address1;

    @JsonProperty("addr2")
    private String address2;

    @JsonProperty("tel")
    private String tel;

    @JsonProperty("mapx")
    private Double longitude;

    @JsonProperty("mapy")
    private Double latitude;

    @JsonProperty("firstimage")
    private String firstImage;

    @JsonProperty("firstimage2")
    private String firstImageThumbnail;

    @JsonProperty("areacode")
    private String areaCode;

    @JsonProperty("sigungucode")
    private String sigunguCode;

    @JsonProperty("cat1")
    private String category1;

    @JsonProperty("cat2")
    private String category2;

    @JsonProperty("cat3")
    private String category3;

    @JsonProperty("modifiedtime")
    private String modifiedTime;

    // --- 반려동물 전용 동반 정보 (Pet Policy Details) ---

    @JsonProperty("acmpyTypeCd")
    private String accompanyTypeCd;

    @JsonProperty("claAcmpyInfo")
    private String petPolicyInfo;

    @JsonProperty("relaAcmpyInfo")
    private String petFacilityInfo;

    @JsonProperty("relaPosbleFclty")
    private String petAllowedFacilities;

    @JsonProperty("relaRqustFclty")
    private String petRequiredItems;

    @JsonProperty("etcAcmpyInfo")
    private String petEtcPolicy;

    @JsonProperty("acmpyPsblCpam")
    private String petWeightLimitText;

    /**
     * 주소 결합 헬퍼 메서드
     */
    public String getFullAddress() {
        if (address2 != null && !address2.isBlank()) {
            return address1 + " " + address2;
        }
        return address1 != null ? address1 : "";
    }
}
