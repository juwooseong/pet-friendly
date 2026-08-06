package com.petspot.domain.place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소 위치 반경 및 복합 조건 검색 Parameter DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceSearchCondition {

    private Double latitude;   // 사용자 중심 위도 (Latitude)
    private Double longitude;  // 사용자 중심 경도 (Longitude)
    private Double radiusKm;   // 반경 거리를 km 단위로 지정 (기본 3.0km)
    private String keyword;    // 장소명/주소 검색 키워드
    private String category;   // 장소 카테고리 ('CAFE', 'HOTEL', 'PARK', 'HOSPITAL', 'SALON')
    private Double maxWeight;  // 동반 가능 최대 체중 (kg)

    public Double getRadiusKmOrDefault() {
        return (radiusKm != null && radiusKm > 0) ? radiusKm : 3.0;
    }
}
