package com.petspot.domain.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * QueryDSL 성능 최적화를 위한 검색 결과 Projection DTO
 */
@Getter
@NoArgsConstructor
public class PlaceSearchResponseDto {

    @Schema(description = "장소 ID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    private UUID id;
    @Schema(description = "공공데이터(TourAPI) contentId (자체 등록 장소는 null)", example = "2930927", nullable = true)
    private String publicDataId;
    @Schema(description = "장소명", example = "멍멍 테라스 카페")
    private String name;
    @Schema(description = "카테고리 코드", example = "CAFE")
    private String category;
    @Schema(description = "카테고리 표시명", example = "카페")
    private String categoryName;
    @Schema(description = "주소", example = "서울특별시 마포구 와우산로 123")
    private String address;
    @Schema(description = "위도", example = "37.5512")
    private Double latitude;
    @Schema(description = "경도", example = "126.9229")
    private Double longitude;
    @Schema(description = "전화번호 (없으면 null)", example = "02-123-4567", nullable = true)
    private String phone;
    @Schema(description = "운영시간 안내(TourAPI detailIntro2에서 정리, 여러 줄). 상세 화면 첫 조회 시 채워지며, 없으면 null", example = "이용 시간: 09:00~18:00\n쉬는 날: 매주 월요일", nullable = true)
    private String operatingHours;
    @Schema(description = "장소 소개(TourAPI 개요). 상세 화면 첫 조회 시 채워지며, 없으면 null", example = "반려견과 함께 산책하기 좋은 숲길입니다.", nullable = true)
    private String description;
    @Schema(description = "대표 이미지 URL (없으면 null)", example = "https://cdn.petspot.example/images/place-001.jpg", nullable = true)
    private String imageUrl;
    @Schema(description = "평균 평점", example = "4.5")
    private BigDecimal rating;
    @Schema(description = "리뷰 수", example = "12")
    private Integer reviewCount;
    @Schema(description = "동반 가능 최대 체중(kg). null=정보 미검증(ADR-007)", example = "10.0", nullable = true)
    private BigDecimal maxWeightLimitKg;
    @Schema(description = "동반 가능 크기 목록. null=정보 미검증, []=허용 크기 없음 확인됨(ADR-007)", nullable = true)
    private List<String> allowedSizes; // 동반 가능 반려동물 크기 목록 ('SMALL', 'MEDIUM', 'LARGE')
    @Schema(description = "검색 중심 좌표로부터의 직선 거리(km). 좌표 미지정 검색 시 null", example = "0.8", nullable = true)
    private Double distanceKm; // 중심 사용자 위치 기준 직선 거리(km)

    @Schema(description = "목록용 작은 대표 이미지 URL (TourAPI firstimage2). 없으면 imageUrl 사용", nullable = true,
            example = "https://tong.visitkorea.or.kr/cms/resource/78/4152278_image3_1.jpg")
    private String thumbnailUrl;

    @Schema(description = "대표 이미지 공공누리 유형 (Type1 출처표시, Type3 출처표시+변경금지 등). 공공데이터 장소만", nullable = true, example = "Type1")
    private String imageCopyrightType;

    @Schema(description = "상세 부가 정보. 키: homepage, parking, infoCenter, babyCarriage, useFee (값이 없는 키는 생략). 상세 화면 첫 조회 시 채워짐", nullable = true)
    private Map<String, String> detailInfo;

    @Schema(description = "상세 사진 목록. 각 항목 키: url, thumbnailUrl, copyrightType. 상세 화면 첫 조회 시 채워짐", nullable = true)
    private List<Map<String, String>> galleryImages;

    @Schema(description = "반려동물 정책 해석 상세 (accompanyScope, leashRequired, carrierRequired, vaccinationRequired, "
            + "muzzleRequiredForDangerousDogs, maxPetCount, allowedPetsText, requirementsText, etcText, facilities, "
            + "furnishedItems, purchasableItems, rentalItems). 근거 없는 항목은 키가 없다(ADR-007)", nullable = true)
    private Map<String, Object> petPolicyDetail;

    @Schema(description = "반려동물 실내 출입 가능 여부. true=가능 확인, false=불가 확인, null=정보 없음(ADR-007)",
            example = "false", nullable = true)
    private Boolean isIndoorAllowed;

    /** 설명(description) 없이 생성 - 기존 호출부 호환용 */
    public PlaceSearchResponseDto(UUID id, String publicDataId, String name, String category,
                                  String categoryName, String address, Double latitude,
                                  Double longitude, String phone, String operatingHours,
                                  String imageUrl, BigDecimal rating, Integer reviewCount,
                                  BigDecimal maxWeightLimitKg, List<String> allowedSizes, Double distanceKm) {
        this(id, publicDataId, name, category, categoryName, address, latitude, longitude, phone, operatingHours,
                null, imageUrl, rating, reviewCount, maxWeightLimitKg, allowedSizes, distanceKm,
                null, null, null, null, null, null);
    }

    @QueryProjection
    public PlaceSearchResponseDto(UUID id, String publicDataId, String name, String category,
                                  String categoryName, String address, Double latitude,
                                  Double longitude, String phone, String operatingHours, String description,
                                  String imageUrl, BigDecimal rating, Integer reviewCount,
                                  BigDecimal maxWeightLimitKg, List<String> allowedSizes, Double distanceKm,
                                  String thumbnailUrl, String imageCopyrightType, Map<String, String> detailInfo,
                                  List<Map<String, String>> galleryImages, Map<String, Object> petPolicyDetail,
                                  Boolean isIndoorAllowed) {
        this.isIndoorAllowed = isIndoorAllowed;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.imageCopyrightType = imageCopyrightType;
        this.detailInfo = detailInfo;
        this.galleryImages = galleryImages;
        this.petPolicyDetail = petPolicyDetail;
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
