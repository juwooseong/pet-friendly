package com.petspot.infrastructure.publicdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TourAPI {@code KorPetTourService2/detailPetTour2}(반려동물 동반여행 조회) 응답 item DTO.
 *
 * <p><b>실제 호출 검증(2026-11-19):</b> 승인된 서비스키로 관광지/레포츠 10곳을 조회한 결과 10곳 모두 아래 필드가
 * 내려왔다(원문 fixture: {@code src/test/resources/tourapi/success-detail-pet-tour2-*-real-2026-11-19.json}).
 * 이 오퍼레이션은 {@code contentId}가 필수이며(없으면 resultCode 11), 목록 조회는 지원하지 않는다 -
 * 장소마다 1회씩 호출해야 한다.
 *
 * <p>과거 {@link TourApiPlaceItemDto}에 "추측 필드"로 들어 있던 {@code acmpyTypeCd}, {@code acmpyPsblCpam},
 * {@code etcAcmpyInfo} 등은 실제로 이 오퍼레이션의 필드였다. TSK-DATA-002(2026-09-09)는 서비스키 승인 전이라
 * 이 경로를 "존재하지 않음"으로 잘못 판정했고, TSK-DATA-005는 이 오퍼레이션을 검증 대상에서 빠뜨렸다(ADR-009).
 *
 * <p>모든 값은 사람이 작성한 자유 텍스트이며, 해석은 {@code TourApiPetPolicyParser}가 담당한다.
 * 필드명 뒤 괄호는 관찰된 실제 값 예시다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiPetTourItemDto {

    @JsonProperty("contentid")
    private String contentId;

    /** 동반 구역 ("전구역 동반가능", "일부구역 동반가능") */
    @JsonProperty("acmpyTypeCd")
    private String accompanyType;

    /** 동반 가능 동물 ("전 견종 동반 가능", "10kg 이하 동반 가능", "15kg 미만 중소형견만 입장 가능(맹견 및 대형견 입장 불가)") */
    @JsonProperty("acmpyPsblCpam")
    private String allowedPets;

    /** 동반 시 필요 사항 ("목줄 착용", "반려동물 유모차 탑승,이동장(켄넬)사용") */
    @JsonProperty("acmpyNeedMtr")
    private String requiredItems;

    /** 기타 동반 정보 (예방접종·실내 출입·마릿수 제한 등 자유 서술) */
    @JsonProperty("etcAcmpyInfo")
    private String etcInfo;

    /**
     * 관련 사고 위험 사항. 관찰된 샘플에서는 대부분 {@code acmpyPsblCpam}과 같은 문구가 들어 있었다
     * (동반 가능 동물이 비어 있을 때 보조로 사용).
     */
    @JsonProperty("relaAcdntRiskMtr")
    private String accidentRiskInfo;

    /** 관련 구비 시설 ("잔디구장, 주차장, 산책로, 샤워실") */
    @JsonProperty("relaPosesFclty")
    private String facilities;

    /** 관련 비치 품목 */
    @JsonProperty("relaFrnshPrdlst")
    private String furnishedItems;

    /** 관련 구매 품목 */
    @JsonProperty("relaPurcPrdlst")
    private String purchasableItems;

    /** 관련 렌탈 품목 */
    @JsonProperty("relaRntlPrdlst")
    private String rentalItems;
}
