package com.petspot.infrastructure.publicdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한국관광공사 TourAPI(KorPetTourService2) 반려동물 동반 장소 항목 개별 DTO
 *
 * <p><b>TSK-DATA-002 검증 상태:</b> 표준 필드(contentid/title/addr1/addr2/tel/mapx/mapy/firstimage/
 * firstimage2/areacode/sigungucode/cat1~3/modifiedtime/contenttypeid)는 TourAPI 4.0 공통 스키마와
 * 동일하여 신뢰도가 높다.
 *
 * <p><b>반려동물 정책 필드 (정정, ADR-009):</b> 과거 이 DTO에는 acmpyTypeCd, acmpyPsblCpam, etcAcmpyInfo 등
 * 반려동물 필드가 "추측 필드"로 들어 있었다. TSK-DATA-005는 이 필드들이 areaBasedList2/detailIntro2/detailInfo2/
 * detailCommon2 응답에 없음을 확인했지만, 실제로는 별도 오퍼레이션 {@code detailPetTour2}의 필드였다
 * (2026-11-19 실제 호출로 확인). 해당 필드는 {@link TourApiPetTourItemDto}로 옮겼다.
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

    // TourAPI 4.0 계열 응답은 숫자형 필드도 JSON 문자열로 내려오는 경우가 흔하고(예: "126.901234"),
    // 좌표 미보유 항목은 빈 문자열("")로 내려오기도 한다. Double로 직접 매핑하면 빈 문자열 항목 하나가
    // 전체 페이지 역직렬화를 실패시키므로 String으로 받아 매퍼에서 안전하게 파싱한다.
    @JsonProperty("mapx")
    private String rawLongitude;

    @JsonProperty("mapy")
    private String rawLatitude;

    /**
     * mapx(경도) 안전 파싱. 값이 없거나 숫자로 변환할 수 없으면 null을 반환한다(예외를 던지지 않음).
     */
    public Double getLongitude() {
        return parseCoordinate(rawLongitude);
    }

    /**
     * mapy(위도) 안전 파싱. 값이 없거나 숫자로 변환할 수 없으면 null을 반환한다(예외를 던지지 않음).
     */
    public Double getLatitude() {
        return parseCoordinate(rawLatitude);
    }

    private static Double parseCoordinate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

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

    // 반려동물 동반 정책 필드는 이 목록 응답(areaBasedList2)에 없다. 장소별 detailPetTour2 응답으로
    // 별도 조회한다 - TourApiPetTourItemDto 참조 (TSK-DATA-007/009, ADR-009).

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
