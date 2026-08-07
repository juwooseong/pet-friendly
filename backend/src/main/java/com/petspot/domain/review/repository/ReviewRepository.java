package com.petspot.domain.review.repository;

import com.petspot.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 리뷰 Spring Data JPA Repository
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /**
     * 특정 장소의 삭제되지 않은 모든 리뷰 목록 조회
     */
    List<Review> findAllByPlaceIdAndDeletedFalse(UUID placeId);

    /**
     * 특정 사용자의 삭제되지 않은 모든 리뷰 목록 조회
     */
    List<Review> findAllByUserIdAndDeletedFalse(UUID userId);

    /**
     * 특정 사용자가 특정 장소에 작성한 삭제되지 않은 리뷰 단건 조회
     */
    Optional<Review> findByUserIdAndPlaceIdAndDeletedFalse(UUID userId, UUID placeId);

    /**
     * 특정 사용자가 특정 장소에 삭제되지 않은 리뷰를 이미 작성했는지 확인
     */
    boolean existsByUserIdAndPlaceIdAndDeletedFalse(UUID userId, UUID placeId);

    /**
     * 특정 장소의 삭제되지 않은 리뷰 총 개수 조회
     */
    long countByPlaceIdAndDeletedFalse(UUID placeId);

    /**
     * 특정 장소의 삭제되지 않은 리뷰 평균 평점 계산 (Double 반환)
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.place.id = :placeId AND r.deleted = false")
    Double averageRatingByPlaceId(@Param("placeId") UUID placeId);

    // 사용자/서비스 편의 메서드 래퍼
    default List<Review> findAllByPlaceId(UUID placeId) {
        return findAllByPlaceIdAndDeletedFalse(placeId);
    }

    default List<Review> findAllByUserId(UUID userId) {
        return findAllByUserIdAndDeletedFalse(userId);
    }

    default Optional<Review> findByUserIdAndPlaceId(UUID userId, UUID placeId) {
        return findByUserIdAndPlaceIdAndDeletedFalse(userId, placeId);
    }

    default boolean existsByUserIdAndPlaceId(UUID userId, UUID placeId) {
        return existsByUserIdAndPlaceIdAndDeletedFalse(userId, placeId);
    }

    default long countByPlaceId(UUID placeId) {
        return countByPlaceIdAndDeletedFalse(placeId);
    }
}
