package com.petspot.domain.place.dto;

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
    private Double latitude;   // 사용자 중심 위도 (Latitude)

    @jakarta.validation.constraints.DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
    @jakarta.validation.constraints.DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
    private Double longitude;  // 사용자 중심 경도 (Longitude)

    @jakarta.validation.constraints.Positive(message = "반경 거리는 0보다 커야 합니다.")
    private Double radiusKm;   // 반경 거리를 km 단위로 지정 (기본 3.0km)

    private String keyword;    // 장소명/주소 검색 키워드
    private String category;   // 장소 카테고리 ('CAFE', 'HOTEL', 'PARK', 'HOSPITAL', 'SALON')

    @jakarta.validation.constraints.PositiveOrZero(message = "체중 제한은 0 이상이어야 합니다.")
    private Double maxWeight;  // 동반 가능 최대 체중 (kg)

    public Double getRadiusKmOrDefault() {
        return (radiusKm != null && radiusKm > 0) ? radiusKm : 3.0;
    }
}
