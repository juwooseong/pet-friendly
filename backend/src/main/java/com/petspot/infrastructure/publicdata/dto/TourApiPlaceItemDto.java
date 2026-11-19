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
 * <p><b>TSK-DATA-005 검증 결과(2026-09-14, 실제 승인 키로 areaBasedList2/detailIntro2/detailInfo2/
 * detailCommon2 실제 호출):</b> 아래 반려동물 전용 필드(acmpyTypeCd, claAcmpyInfo, relaAcmpyInfo,
 * relaPosbleFclty, relaRqustFclty, etcAcmpyInfo, acmpyPsblCpam)는 <b>실제 응답 어디에도 존재하지 않음이
 * 확인되었다</b> (contentTypeId=38/쇼핑, contentTypeId=32/숙박 두 종류 모두 미존재). 즉 areaBasedList2/
 * detailIntro2/detailInfo2/detailCommon2 네 오퍼레이션 전체를 통틀어 이 필드들은 실물 스키마가 아니다 -
 * 최초 구현 시점의 추측치가 그대로 남아 있던 것으로 확인됨. detailIntro2에서 contentTypeId=38(쇼핑)에
 * 한해 "chkpetshopping"(반려동물 동반 가능 여부, 값은 관찰된 샘플에서 빈 문자열)이라는 필드가 존재하지만,
 * contentTypeId=32(숙박)의 detailIntro2에는 그런 chkpet류 필드 자체가 없다 - 즉 필드 유무/이름이
 * contentTypeId별로 다르다. 체중 제한(kg)/크기/목줄/이동장(케이지)/실내외/오프리쉬 관련 필드도 확인된
 * 4개 오퍼레이션 어디에도 없었다. 상세 근거와 fixture 목록은 docs/TASKS.md TSK-DATA-005 기록 및
 * src/test/resources/tourapi/success-detail-*-real-2026-09-14.json 참조.
 *
 * <p>이 필드들은 실제로 존재하지 않는 것으로 검증되었지만, TSK-DATA-005 범위 제한(Entity/매핑 로직 변경
 * 금지)에 따라 이 DTO 자체는 아직 수정하지 않았다 - 제거/교체는 별도 작업에서 처리한다.
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
    // [주의] TSK-DATA-005(2026-09-14) 실제 호출 검증 결과, 아래 7개 필드는 TourAPI 실제 응답에 존재하지 않는
    // 최초 구현 당시의 추측 필드다. 실제 응답에서는 항상 null로 역직렬화되며, 실데이터 근거로 사용하면 안 된다.
    // 제거/교체는 TSK-DATA-007(백로그)에서 처리한다.

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
