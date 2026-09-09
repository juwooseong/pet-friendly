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
 * 동일하여 신뢰도가 높다. 다만 아래 반려동물 전용 필드(acmpyTypeCd 등)는 이 작업에서 실제 성공 응답으로
 * 실물 검증을 완료하지 못했다 — 로컬 프로필 시크릿이 data.go.kr 게이트웨이에서
 * SERVICE_KEY_IS_NOT_REGISTERED_ERROR(reasonCode 30)를 반환해(활용신청 승인 대기/미승인 추정) 실제
 * item 페이로드를 받지 못했고, 이 필드명들은 기존 코드에 이미 존재하던 가정치를 그대로 유지한 것이다.
 * 체중 제한(kg) 수치 전용 필드가 실제로 존재하는지도 마찬가지로 미확인이며, 현재는 자유 텍스트
 * (acmpyPsblCpam 등)에서 정규식으로 추출하는 방식에 의존한다. 키 승인 후 재검증 필요 (백로그 참조).
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
