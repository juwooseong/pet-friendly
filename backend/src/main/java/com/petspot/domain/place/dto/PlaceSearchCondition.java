package com.petspot.domain.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 장소 위치 반경 및 복합 조건 검색 Parameter DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceSearchCondition {

    @jakarta.validation.constraints.DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
    @jakarta.validation.constraints.DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
    @Schema(description = "사용자 중심 위도 (-90~90). longitude와 함께 지정하면 반경 검색 + 거리순 정렬", example = "37.5665")
    private Double latitude;   // 사용자 중심 위도 (Latitude)

    @jakarta.validation.constraints.DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
    @jakarta.validation.constraints.DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
    @Schema(description = "사용자 중심 경도 (-180~180)", example = "126.9780")
    private Double longitude;  // 사용자 중심 경도 (Longitude)

    @jakarta.validation.constraints.Positive(message = "반경 거리는 0보다 커야 합니다.")
    @Schema(description = "반경(km, 0 초과). 미지정 시 3.0km", example = "3.0")
    private Double radiusKm;   // 반경 거리를 km 단위로 지정 (기본 3.0km)

    @Schema(description = "장소명/주소 검색 키워드", example = "카페")
    private String keyword;    // 장소명/주소 검색 키워드
    @Schema(description = "장소 카테고리 (CAFE, HOTEL, PARK, HOSPITAL, SALON)", example = "CAFE")
    private String category;   // 장소 카테고리 ('CAFE', 'HOTEL', 'PARK', 'HOSPITAL', 'SALON')

    @jakarta.validation.constraints.PositiveOrZero(message = "체중 제한은 0 이상이어야 합니다.")
    @Schema(description = "반려동물 체중(kg, 0 이상). 장소 체중 제한 이하인 곳만 조회", example = "10")
    private Double maxWeight;  // 동반 가능 최대 체중 (kg)

    @jakarta.validation.constraints.Pattern(regexp = "SMALL|MEDIUM|LARGE", message = "sizeCategory는 SMALL, MEDIUM, LARGE 중 하나여야 합니다.")
    @Schema(description = "반려동물 크기 (SMALL, MEDIUM, LARGE). 허용이 확인된 장소(allowedSizes에 포함)를 먼저, "
            + "크기 정보가 없는 장소(allowedSizes=null)를 그 뒤에 보여주고, 해당 크기가 허용되지 않는다고 확인된 장소만 제외한다. "
            + "sizeVerifiedOnly=true면 허용이 확인된 장소만 조회", example = "SMALL", nullable = true)
    private String sizeCategory; // 반려동물 크기 조건 ('SMALL', 'MEDIUM', 'LARGE')

    @Schema(description = "true면 sizeCategory 허용이 확인된 장소만 조회(크기 정보 없는 장소 제외). 기본 false", example = "false", nullable = true)
    private Boolean sizeVerifiedOnly; // TSK-FE-019: 크기 정보 미확인 장소 포함 여부

    public boolean isSizeVerifiedOnlyEnabled() {
        return Boolean.TRUE.equals(sizeVerifiedOnly);
    }

    public Double getRadiusKmOrDefault() {
        return (radiusKm != null && radiusKm > 0) ? radiusKm : 3.0;
    }
}
