package com.petspot.domain.place.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 반려동물 동반 장소 JPA 엔티티 (PostGIS Location 포함)
 */
@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "public_data_id", unique = true, length = 100)
    private String publicDataId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "location", nullable = false, columnDefinition = "GEOMETRY(Point, 4326)")
    private Point location;

    @Column(name = "latitude", nullable = false, columnDefinition = "NUMERIC(10, 7)")
    private Double latitude;

    @Column(name = "longitude", nullable = false, columnDefinition = "NUMERIC(10, 7)")
    private Double longitude;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "operating_hours", columnDefinition = "TEXT")
    private String operatingHours;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 리뷰가 하나도 없는 장소의 평점 값 (TSK-BE-032) */
    public static final BigDecimal NO_REVIEW_RATING = new BigDecimal("0.0");

    /** 리뷰 평균 평점(소수 첫째 자리). 리뷰가 없으면 {@link #NO_REVIEW_RATING} */
    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "max_weight_limit_kg", precision = 4, scale = 1)
    private BigDecimal maxWeightLimitKg;

    /**
     * 동반 가능 반려동물 크기 목록 (JSONB, 예: ["SMALL", "MEDIUM"])
     * PetSizeCategory 문자열 값으로 저장된다.
     *
     * <p><b>TSK-DATA-008 (ADR-007) NULL 정책:</b> {@code null} = 아직 검증되지 않음(미확인),
     * 빈 리스트 {@code []} = 실제 근거로 "허용 크기 없음"이 확인됨, 값이 채워진 리스트 = 실제
     * 근거로 확인된 허용 크기 목록. 근거 없이 전체 크기 허용으로 채우지 않는다 - 과거에는 생성자가
     * {@code null} 입력을 자동으로 전체 허용 목록으로 치환했으나, 이는 "정보 없음"과 "실제로 전체
     * 허용됨"을 구분하지 못하게 하는 버그였다(TSK-DATA-006 ADR-007에서 식별, 이번 작업에서 제거).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_sizes", columnDefinition = "jsonb")
    private List<String> allowedSizes;

    /**
     * <b>TSK-DATA-008 (ADR-007) NULL 정책:</b> {@code null} = 아직 검증되지 않음(미확인),
     * {@code false} = 실제 근거로 불가능함이 확인됨, {@code true} = 실제 근거로 가능함이 확인됨.
     */
    @Column(name = "is_indoor_allowed")
    private Boolean isIndoorAllowed;

    /** {@link #isIndoorAllowed}와 동일한 NULL 정책을 따른다. */
    @Column(name = "is_outdoor_allowed")
    private Boolean isOutdoorAllowed;

    /** {@link #isIndoorAllowed}와 동일한 NULL 정책을 따른다. */
    @Column(name = "has_off_leash_zone")
    private Boolean hasOffLeashZone;

    /** 정책 출처: TourAPI detailPetTour2 자동 반영 (TSK-DATA-010, ADR-009) */
    public static final String POLICY_SOURCE_TOURAPI = "TOURAPI";
    /** 정책 출처: 관리자가 검증한 값 - TourAPI 재동기화로 덮어쓰지 않는다 */
    public static final String POLICY_SOURCE_ADMIN = "ADMIN";

    /**
     * 전용 컬럼이 없는 반려동물 정책 해석 결과와 원문 (예: accompanyScope, leashRequired, carrierRequired,
     * vaccinationRequired, maxPetCount, allowedPetsText, requirementsText, etcText). 근거 없는 항목은 키 자체가 없다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pet_policy_detail", columnDefinition = "jsonb")
    private Map<String, Object> petPolicyDetail;

    /** 정책 값 출처 ({@link #POLICY_SOURCE_TOURAPI} / {@link #POLICY_SOURCE_ADMIN}), 정책이 없으면 null */
    @Column(name = "pet_policy_source", length = 20)
    private String petPolicySource;

    /** 마지막 TourAPI 정책 조회 시각. null이면 아직 조회 전이라 배치가 우선 처리한다 */
    @Column(name = "pet_policy_synced_at")
    private ZonedDateTime petPolicySyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Builder
    public Place(UUID id, String publicDataId, String name, String category, String categoryName,
                 String address, Point location, Double latitude, Double longitude, String phone,
                 String operatingHours, String imageUrl, String description, BigDecimal rating,
                 Integer reviewCount, BigDecimal maxWeightLimitKg, List<String> allowedSizes,
                 Boolean isIndoorAllowed, Boolean isOutdoorAllowed, Boolean hasOffLeashZone) {
        this.id = id;
        this.publicDataId = publicDataId;
        this.name = name;
        this.category = category;
        this.categoryName = categoryName != null ? categoryName : category;
        this.address = address;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.operatingHours = operatingHours;
        this.imageUrl = imageUrl;
        this.description = description;
        // TSK-BE-032: 리뷰가 없는 장소의 평점은 0.0으로 통일한다(리뷰 전부 삭제 시 재계산 결과와 동일).
        // 과거 기본값 5.0은 리뷰 없는 장소를 만점으로 노출하고 인기 장소(평점 내림차순) 상위에 올리는 문제가 있었다.
        // 화면에서는 reviewCount == 0 이면 평점 대신 "리뷰 없음"으로 표시한다.
        this.rating = rating != null ? rating : NO_REVIEW_RATING;
        this.reviewCount = reviewCount != null ? reviewCount : 0;
        // TSK-DATA-008 (ADR-007): 반려동물 정책 필드는 실제 근거 없이 기본값을 채우지 않는다.
        // null 입력을 그대로 유지한다("아직 검증되지 않음") - maxWeightLimitKg와 동일한 정책.
        this.maxWeightLimitKg = maxWeightLimitKg;
        this.allowedSizes = allowedSizes;
        this.isIndoorAllowed = isIndoorAllowed;
        this.isOutdoorAllowed = isOutdoorAllowed;
        this.hasOffLeashZone = hasOffLeashZone;
    }

    /**
     * 기존 장소 데이터 수정을 위한 도메인 메서드 (Setter 금지)
     */
    public void updateDetails(String name, String address, Point location, Double latitude, Double longitude,
                              String imageUrl, String phone, String categoryName) {
        this.name = name;
        this.address = address;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        if (imageUrl != null && !imageUrl.isBlank()) {
            this.imageUrl = imageUrl;
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone;
        }
        if (categoryName != null && !categoryName.isBlank()) {
            this.categoryName = categoryName;
        }
    }

    /**
     * TourAPI {@code detailPetTour2}에서 해석한 반려동물 정책을 반영한다 (TSK-DATA-010, ADR-009).
     *
     * <p>관리자가 검증한 정책({@link #POLICY_SOURCE_ADMIN})은 덮어쓰지 않고 조회 시각만 기록한다.
     * 해석하지 못한 항목은 null 그대로 반영한다(ADR-007 - 근거 없는 기본값 금지).
     *
     * @return 정책 값을 실제로 갱신했으면 true, 관리자 값 보호로 건너뛰었으면 false
     */
    public boolean applyTourApiPetPolicy(BigDecimal maxWeightLimitKg, List<String> allowedSizes, Boolean indoorAllowed,
                                         Map<String, Object> policyDetail, ZonedDateTime syncedAt) {
        this.petPolicySyncedAt = syncedAt;
        if (POLICY_SOURCE_ADMIN.equals(this.petPolicySource)) {
            return false;
        }
        this.maxWeightLimitKg = maxWeightLimitKg;
        this.allowedSizes = allowedSizes;
        this.isIndoorAllowed = indoorAllowed;
        this.petPolicyDetail = policyDetail;
        this.petPolicySource = POLICY_SOURCE_TOURAPI;
        return true;
    }

    /**
     * TourAPI에 이 장소의 반려동물 정책이 등록되어 있지 않음을 기록한다.
     * 이전에 TourAPI에서 가져온 정책이 있었다면(출처 TOURAPI) 더 이상 근거가 없으므로 비운다. 관리자 값은 유지한다.
     */
    public void markTourApiPetPolicyNotProvided(ZonedDateTime syncedAt) {
        this.petPolicySyncedAt = syncedAt;
        if (POLICY_SOURCE_TOURAPI.equals(this.petPolicySource)) {
            this.maxWeightLimitKg = null;
            this.allowedSizes = null;
            this.isIndoorAllowed = null;
            this.petPolicyDetail = null;
            this.petPolicySource = null;
        }
    }

    /**
     * 리뷰 등록/수정/삭제 시 평균 평점 및 리뷰 개수를 갱신하는 도메인 메서드
     */
    public void updateRatingAndReviewCount(BigDecimal newRating, Integer newReviewCount) {
        this.rating = newRating != null ? newRating : NO_REVIEW_RATING;
        this.reviewCount = newReviewCount != null ? newReviewCount : 0;
    }
}
