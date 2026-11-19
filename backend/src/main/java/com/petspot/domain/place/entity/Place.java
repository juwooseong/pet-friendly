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
        this.rating = rating != null ? rating : BigDecimal.valueOf(5.0);
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
     * 리뷰 등록/수정/삭제 시 평균 평점 및 리뷰 개수를 갱신하는 도메인 메서드
     */
    public void updateRatingAndReviewCount(BigDecimal newRating, Integer newReviewCount) {
        this.rating = newRating != null ? newRating : BigDecimal.valueOf(0.0);
        this.reviewCount = newReviewCount != null ? newReviewCount : 0;
    }
}
