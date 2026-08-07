package com.petspot.application.review;

import com.petspot.api.review.dto.ReviewCreateRequestDto;
import com.petspot.api.review.dto.ReviewResponseDto;
import com.petspot.api.review.dto.ReviewUpdateRequestDto;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.domain.review.entity.Review;
import com.petspot.domain.review.repository.ReviewRepository;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.DuplicateReviewException;
import com.petspot.global.error.exception.PlaceNotFoundException;
import com.petspot.global.error.exception.ReviewAccessDeniedException;
import com.petspot.global.error.exception.ReviewNotFoundException;
import com.petspot.global.error.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * 장소 리뷰 CUD 및 조회 애플리케이션 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    /**
     * 신규 리뷰 작성 및 Place 평균 평점/리뷰 수 동기화
     */
    @Transactional
    public ReviewResponseDto createReview(UUID userId, ReviewCreateRequestDto request) {
        log.info("[REVIEW CREATE] Creating review for userId: {}, placeId: {}", userId, request.getPlaceId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다.");
        }

        Place place = placeRepository.findById(request.getPlaceId())
                .orElseThrow(() -> new PlaceNotFoundException("장소를 찾을 수 없습니다."));

        // 장소당 한 번만 작성 가능 (중복 검사)
        if (reviewRepository.existsByUserIdAndPlaceId(userId, request.getPlaceId())) {
            log.warn("[REVIEW CREATE FAILED] Duplicate review attempt by userId: {} for placeId: {}", userId, request.getPlaceId());
            throw new DuplicateReviewException("이미 이 장소에 리뷰를 작성하셨습니다.");
        }

        Review review = Review.create(user, place, request.getRating(), request.getContent(), request.getImageUrl());
        Review savedReview = reviewRepository.save(review);

        // Place 집계 정보 동기화
        updatePlaceRatingAndReviewCount(place.getId());

        log.info("[REVIEW CREATE SUCCESS] Created reviewId: {} for placeId: {}", savedReview.getId(), place.getId());
        return ReviewResponseDto.from(savedReview);
    }

    /**
     * 리뷰 수정 및 Place 평균 평점 동기화
     */
    @Transactional
    public ReviewResponseDto updateReview(UUID userId, UUID reviewId, ReviewUpdateRequestDto request) {
        log.info("[REVIEW UPDATE] Updating reviewId: {} for userId: {}", reviewId, userId);

        Review review = findActiveReviewAndValidateOwner(userId, reviewId);

        review.updateReview(request.getRating(), request.getContent(), request.getImageUrl());

        // Place 집계 정보 동기화
        updatePlaceRatingAndReviewCount(review.getPlace().getId());

        log.info("[REVIEW UPDATE SUCCESS] Updated reviewId: {}", reviewId);
        return ReviewResponseDto.from(review);
    }

    /**
     * 리뷰 삭제 (Soft Delete) 및 Place 평균 평점/리뷰 수 동기화
     */
    @Transactional
    public void deleteReview(UUID userId, UUID reviewId) {
        log.info("[REVIEW DELETE] Deleting reviewId: {} for userId: {}", reviewId, userId);

        Review review = findActiveReviewAndValidateOwner(userId, reviewId);

        review.delete(); // Soft Delete (deleted = true)

        // Place 집계 정보 동기화
        updatePlaceRatingAndReviewCount(review.getPlace().getId());

        log.info("[REVIEW DELETE SUCCESS] Soft deleted reviewId: {}", reviewId);
    }

    /**
     * 특정 장소의 모든 삭제되지 않은 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getPlaceReviews(UUID placeId) {
        log.info("[REVIEW GET PLACE] Fetching reviews for placeId: {}", placeId);

        if (!placeRepository.existsById(placeId)) {
            throw new PlaceNotFoundException("장소를 찾을 수 없습니다.");
        }

        List<Review> reviews = reviewRepository.findAllByPlaceId(placeId);
        return reviews.stream()
                .map(ReviewResponseDto::from)
                .toList();
    }

    /**
     * 내가 작성한 모든 삭제되지 않은 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getMyReviews(UUID userId) {
        log.info("[REVIEW GET USER] Fetching reviews for userId: {}", userId);

        List<Review> reviews = reviewRepository.findAllByUserId(userId);
        return reviews.stream()
                .map(ReviewResponseDto::from)
                .toList();
    }

    /**
     * 리뷰 변경 시 Place 엔티티의 rating 및 reviewCount를 동기화하는 도메인 헬퍼 메서드
     */
    private void updatePlaceRatingAndReviewCount(UUID placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new PlaceNotFoundException("장소를 찾을 수 없습니다."));

        Double avgRatingDouble = reviewRepository.averageRatingByPlaceId(placeId);
        long count = reviewRepository.countByPlaceId(placeId);

        BigDecimal avgRating = avgRatingDouble != null
                ? BigDecimal.valueOf(avgRatingDouble).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(0.0);

        place.updateRatingAndReviewCount(avgRating, (int) count);
        log.debug("[PLACE RATING SYNCED] placeId: {}, newRating: {}, newCount: {}", placeId, avgRating, count);
    }

    /**
     * 리뷰 존재 확인, Soft Delete 여부 및 소유자 권한 검증 헬퍼 메서드
     */
    private Review findActiveReviewAndValidateOwner(UUID userId, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("리뷰를 찾을 수 없습니다."));

        if (review.isDeleted()) {
            throw new ReviewNotFoundException("리뷰를 찾을 수 없습니다.");
        }

        if (!review.getUser().getId().equals(userId)) {
            log.warn("[REVIEW ACCESS DENIED] userId: {} attempted to modify reviewId: {} owned by userId: {}", userId, reviewId, review.getUser().getId());
            throw new ReviewAccessDeniedException("해당 리뷰를 수정 또는 삭제할 권한이 없습니다.");
        }

        return review;
    }
}
