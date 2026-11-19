package com.petspot.infrastructure.publicdata.mapper;

import java.math.BigDecimal;
import java.util.List;

/**
 * TourAPI {@code detailPetTour2} 자유 텍스트에서 해석한 반려동물 동반 정책.
 *
 * <p><b>ADR-007 NULL 원칙을 그대로 따른다:</b> 텍스트에서 근거를 찾지 못한 항목은 {@code null}(미확인)로 둔다.
 * 예를 들어 목줄 언급이 없으면 {@code leashRequired=null}이지 {@code false}가 아니다.
 *
 * @param accompanyScope      동반 구역 (ALL_AREAS / PARTIAL_AREAS, 미확인 null)
 * @param maxWeightLimitKg    동반 가능 최대 체중(kg). "N kg 미만"도 N으로 저장한다(경계값 1건 차이는 감수, 원문은 별도 보존)
 * @param allowedSizes        동반 가능 크기 (SMALL/MEDIUM/LARGE, PetSizeCategory 기준과 동일한 체중 경계)
 * @param maxPetCount         1인(또는 객실)당 최대 동반 마릿수
 * @param leashRequired       목줄/리드줄 필수 여부
 * @param carrierRequired     이동장/켄넬/이동가방/유모차 필수 여부
 * @param vaccinationRequired 예방접종 완료 필수 여부
 * @param muzzleRequiredForDangerousDogs 맹견 입마개 필수 여부
 * @param indoorAllowed       실내 출입 가능 여부 ("실내 ... 불가" 명시 시에만 false)
 * @param allowedPetsText     원문: 동반 가능 동물
 * @param requirementsText    원문: 동반 시 필요 사항
 * @param etcText             원문: 기타 동반 정보
 */
public record ParsedPetPolicy(
        AccompanyScope accompanyScope,
        BigDecimal maxWeightLimitKg,
        List<String> allowedSizes,
        Integer maxPetCount,
        Boolean leashRequired,
        Boolean carrierRequired,
        Boolean vaccinationRequired,
        Boolean muzzleRequiredForDangerousDogs,
        Boolean indoorAllowed,
        String allowedPetsText,
        String requirementsText,
        String etcText
) {

    public enum AccompanyScope {
        /** 전구역 동반가능 */
        ALL_AREAS,
        /** 일부구역 동반가능 */
        PARTIAL_AREAS
    }

    /** 해석할 원문이 전혀 없는 경우(전 항목 미확인) */
    public static ParsedPetPolicy unknown() {
        return new ParsedPetPolicy(null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
