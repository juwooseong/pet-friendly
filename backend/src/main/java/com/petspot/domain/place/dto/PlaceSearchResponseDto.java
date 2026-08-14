package com.petspot.domain.place.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * QueryDSL 성능 최적화를 위한 검색 결과 Projection DTO
 */
@Getter
@NoArgsConstructor
public class PlaceSearchResponseDto {

    private UUID id;
    private String publicDataId;
    private String name;
    private String category;
    private String categoryName;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String operatingHours;
    private String imageUrl;
    private BigDecimal rating;
    private Integer reviewCount;
    private BigDecimal maxWeightLimitKg;
    private List<String> allowedSizes; // 동반 가능 반려동물 크기 목록 ('SMALL', 'MEDIUM', 'LARGE')
    private Double distanceKm; // 중심 사용자 위치 기준 직선 거리(km)

    @QueryProjection
    public PlaceSearchResponseDto(UUID id, String publicDataId, String name, String category,
                                  String categoryName, String address, Double latitude,
                                  Double longitude, String phone, String operatingHours,
                                  String imageUrl, BigDecimal rating, Integer reviewCount,
                                  BigDecimal maxWeightLimitKg, List<String> allowedSizes, Double distanceKm) {
        this.id = id;
        this.publicDataId = publicDataId;
        this.name = name;
        this.category = category;
        this.categoryName = categoryName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.operatingHours = operatingHours;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.maxWeightLimitKg = maxWeightLimitKg;
        this.allowedSizes = allowedSizes;
        this.distanceKm = distanceKm != null ? Math.round(distanceKm * 100.0) / 100.0 : null; // 소수점둘째자리
    }
}
