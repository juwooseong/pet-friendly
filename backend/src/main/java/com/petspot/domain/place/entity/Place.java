package com.petspot.domain.place.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
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

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
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

    @Column(name = "is_indoor_allowed")
    private Boolean isIndoorAllowed;

    @Column(name = "is_outdoor_allowed")
    private Boolean isOutdoorAllowed;

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
                 Integer reviewCount, BigDecimal maxWeightLimitKg, Boolean isIndoorAllowed,
                 Boolean isOutdoorAllowed, Boolean hasOffLeashZone) {
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
        this.maxWeightLimitKg = maxWeightLimitKg;
        this.isIndoorAllowed = isIndoorAllowed != null ? isIndoorAllowed : true;
        this.isOutdoorAllowed = isOutdoorAllowed != null ? isOutdoorAllowed : true;
        this.hasOffLeashZone = hasOffLeashZone != null ? hasOffLeashZone : false;
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
}
