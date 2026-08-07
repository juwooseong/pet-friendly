package com.petspot.application.review;

import com.petspot.api.review.dto.ReviewCreateRequestDto;
import com.petspot.api.review.dto.ReviewResponseDto;
import com.petspot.api.review.dto.ReviewUpdateRequestDto;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.domain.review.entity.Review;
import com.petspot.domain.review.repository.ReviewRepository;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.DuplicateReviewException;
import com.petspot.global.error.exception.ReviewAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User mockUser;
    private User otherUser;
    private Place mockPlace;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockUser = User.register("user@petspot.com", "hash", "리뷰어");
        otherUser = User.register("other@petspot.com", "hash", "타인");

        mockPlace = Place.builder()
                .id(UUID.randomUUID())
                .name("멍멍 카페")
                .rating(BigDecimal.valueOf(5.0))
                .reviewCount(0)
                .build();
    }

    @Test
    @DisplayName("신규 리뷰 등록 성공 및 Place 평점/리뷰 수 동기화 검증")
    void createReview_Success() {
        // given
        UUID userId = mockUser.getId();
        UUID placeId = mockPlace.getId();

        ReviewCreateRequestDto request = ReviewCreateRequestDto.builder()
                .placeId(placeId)
                .rating(5)
                .content("정말 친절하고 깨끗해요!")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(placeRepository.findById(placeId)).willReturn(Optional.of(mockPlace));
        given(reviewRepository.existsByUserIdAndPlaceId(userId, placeId)).willReturn(false);

        Review mockSavedReview = Review.create(mockUser, mockPlace, 5, "정말 친절하고 깨끗해요!", null);
        given(reviewRepository.save(any(Review.class))).willReturn(mockSavedReview);

        given(reviewRepository.averageRatingByPlaceId(placeId)).willReturn(5.0);
        given(reviewRepository.countByPlaceId(placeId)).willReturn(1L);

        // when
        ReviewResponseDto response = reviewService.createReview(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getContent()).isEqualTo("정말 친절하고 깨끗해요!");
        assertThat(mockPlace.getRating()).isEqualTo(BigDecimal.valueOf(5.0));
        assertThat(mockPlace.getReviewCount()).isEqualTo(1);

        verify(reviewRepository).save(any(Review.class));
        verify(reviewRepository).averageRatingByPlaceId(placeId);
    }

    @Test
    @DisplayName("동일 장소 중복 리뷰 등록 시 DuplicateReviewException 예외 발생")
    void createReview_Duplicate_ThrowsException() {
        // given
        UUID userId = mockUser.getId();
        UUID placeId = mockPlace.getId();

        ReviewCreateRequestDto request = ReviewCreateRequestDto.builder()
                .placeId(placeId)
                .rating(4)
                .content("두 번째 리뷰 시도")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(placeRepository.findById(placeId)).willReturn(Optional.of(mockPlace));
        given(reviewRepository.existsByUserIdAndPlaceId(userId, placeId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(userId, request))
                .isInstanceOf(DuplicateReviewException.class)
                .hasMessage("이미 이 장소에 리뷰를 작성하셨습니다.");
    }

    @Test
    @DisplayName("리뷰 수정 성공 및 Place 평점 동기화 검증")
    void updateReview_Success() {
        // given
        UUID userId = mockUser.getId();
        Review review = Review.create(mockUser, mockPlace, 3, "보통이에요", null);
        UUID reviewId = review.getId();

        ReviewUpdateRequestDto request = ReviewUpdateRequestDto.builder()
                .rating(5)
                .content("생각해보니 정말 좋았습니다!")
                .build();

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(placeRepository.findById(mockPlace.getId())).willReturn(Optional.of(mockPlace));
        given(reviewRepository.averageRatingByPlaceId(mockPlace.getId())).willReturn(5.0);
        given(reviewRepository.countByPlaceId(mockPlace.getId())).willReturn(1L);

        // when
        ReviewResponseDto response = reviewService.updateReview(userId, reviewId, request);

        // then
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getContent()).isEqualTo("생각해보니 정말 좋았습니다!");
        assertThat(mockPlace.getRating()).isEqualTo(BigDecimal.valueOf(5.0));
    }

    @Test
    @DisplayName("리뷰 삭제 (Soft Delete) 성공 및 Place 평점/개수 재계산 동기화 검증")
    void deleteReview_Success() {
        // given
        UUID userId = mockUser.getId();
        Review review = Review.create(mockUser, mockPlace, 4, "삭제할 리뷰", null);
        UUID reviewId = review.getId();

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(placeRepository.findById(mockPlace.getId())).willReturn(Optional.of(mockPlace));
        given(reviewRepository.averageRatingByPlaceId(mockPlace.getId())).willReturn(null);
        given(reviewRepository.countByPlaceId(mockPlace.getId())).willReturn(0L);

        // when
        reviewService.deleteReview(userId, reviewId);

        // then
        assertThat(review.isDeleted()).isTrue();
        assertThat(mockPlace.getRating()).isEqualTo(BigDecimal.valueOf(0.0));
        assertThat(mockPlace.getReviewCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("타인의 리뷰 수정 시도 시 ReviewAccessDeniedException 발생")
    void updateReview_OtherUser_ThrowsException() {
        // given
        UUID userId = mockUser.getId();
        Review otherReview = Review.create(otherUser, mockPlace, 4, "남의 리뷰", null);
        UUID reviewId = otherReview.getId();

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(otherReview));

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(userId, reviewId, ReviewUpdateRequestDto.builder().rating(5).content("수정시도").build()))
                .isInstanceOf(ReviewAccessDeniedException.class)
                .hasMessage("해당 리뷰를 수정 또는 삭제할 권한이 없습니다.");
    }
}
