package com.petspot.domain.review.entity;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 장소 리뷰 JPA 엔티티 (User와 Place 연결 Aggregate, Soft Delete 적용)
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reviews_user_place", columnNames = {"user_id", "place_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Builder(access = AccessLevel.PROTECTED)
    protected Review(UUID id, User user, Place place, Integer rating, String content, String imageUrl, Boolean deleted) {
        validateRating(rating);
        validateContent(content);

        this.id = id != null ? id : UUID.randomUUID();
        this.user = user;
        this.place = place;
        this.rating = rating;
        this.content = content;
        this.imageUrl = imageUrl;
        this.deleted = deleted != null ? deleted : false;
    }

    /**
     * 리뷰 생성 정적 팩토리 메서드
     */
    public static Review create(User user, Place place, Integer rating, String content, String imageUrl) {
        return Review.builder()
                .user(user)
                .place(place)
                .rating(rating)
                .content(content)
                .imageUrl(imageUrl)
                .deleted(false)
                .build();
    }

    /**
     * 리뷰 내용 및 평점 수정 (도메인 메서드)
     */
    public void updateReview(Integer rating, String content, String imageUrl) {
        if (rating != null) {
            validateRating(rating);
            this.rating = rating;
        }
        if (content != null) {
            validateContent(content);
            this.content = content;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
    }

    /**
     * 리뷰 삭제 (Soft Delete 도메인 메서드)
     */
    public void delete() {
        this.deleted = true;
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1점 이상 5점 이하이어야 합니다.");
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > 1000) {
            throw new IllegalArgumentException("리뷰 내용은 1자 이상 1000자 이하이어야 합니다.");
        }
    }
}
