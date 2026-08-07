package com.petspot.domain.review.repository;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.review.entity.Review;
import com.petspot.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class ReviewRepositoryTest {

    @Mock
    private ReviewRepository reviewRepository;

    private User mockUser;
    private Place mockPlace;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        mockUser = User.register("reviewer@petspot.com", "hash", "리뷰어");

        Point location = geometryFactory.createPoint(new Coordinate(127.0276, 37.4979));
        mockPlace = Place.builder()
                .id(UUID.randomUUID())
                .name("멍멍 애견파크")
                .category("PARK")
                .categoryName("여행/숙박 > 애견파크")
                .address("서울시 서초구 서초대로 456")
                .location(location)
                .latitude(37.4979)
                .longitude(127.0276)
                .build();
    }

    @Test
    @DisplayName("Review 엔티티 팩토리 생성 및 평점/내용 유효성 검증 성공")
    void reviewEntity_Create_Success() {
        // given
        Review review = Review.create(mockUser, mockPlace, 5, "강아지가 정말 좋아해요!", "https://example.com/review.jpg");

        // then
        assertThat(review).isNotNull();
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getContent()).isEqualTo("강아지가 정말 좋아해요!");
        assertThat(review.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("유효하지 않은 평점(0점 또는 6점) 입력 시 IllegalArgumentException 발생")
    void reviewEntity_InvalidRating_ThrowsException() {
        assertThatThrownBy(() -> Review.create(mockUser, mockPlace, 0, "좋아요", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평점은 1점 이상 5점 이하이어야 합니다.");

        assertThatThrownBy(() -> Review.create(mockUser, mockPlace, 6, "좋아요", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평점은 1점 이상 5점 이하이어야 합니다.");
    }

    @Test
    @DisplayName("Review 도메인 메서드(updateReview, delete) 기능 검증")
    void reviewEntity_DomainMethods_Success() {
        // given
        Review review = Review.create(mockUser, mockPlace, 4, "괜찮아요", null);

        // when 1: 수정
        review.updateReview(5, "최고로 변경합니다!", "https://example.com/new.jpg");
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getContent()).isEqualTo("최고로 변경합니다!");

        // when 2: Soft Delete
        review.delete();
        assertThat(review.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("특정 장소의 삭제되지 않은 리뷰 목록 조회 (findAllByPlaceId) Mock 테스트")
    void findAllByPlaceId_Success() {
        // given
        UUID placeId = mockPlace.getId();
        Review review = Review.create(mockUser, mockPlace, 5, "최고의 장소", null);

        given(reviewRepository.findAllByPlaceId(placeId)).willReturn(List.of(review));

        // when
        List<Review> reviews = reviewRepository.findAllByPlaceId(placeId);

        // then
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).getContent()).isEqualTo("최고의 장소");
        verify(reviewRepository).findAllByPlaceId(placeId);
    }

    @Test
    @DisplayName("특정 장소의 삭제되지 않은 리뷰 평균 평점 계산 (averageRatingByPlaceId) Mock 테스트")
    void averageRatingByPlaceId_Success() {
        // given
        UUID placeId = mockPlace.getId();
        given(reviewRepository.averageRatingByPlaceId(placeId)).willReturn(4.5);

        // when
        Double avgRating = reviewRepository.averageRatingByPlaceId(placeId);

        // then
        assertThat(avgRating).isEqualTo(4.5);
        verify(reviewRepository).averageRatingByPlaceId(placeId);
    }

    @Test
    @DisplayName("특정 사용자의 중복 리뷰 등록 여부 확인 (existsByUserIdAndPlaceId) Mock 테스트")
    void existsByUserIdAndPlaceId_Success() {
        // given
        UUID userId = mockUser.getId();
        UUID placeId = mockPlace.getId();

        given(reviewRepository.existsByUserIdAndPlaceId(userId, placeId)).willReturn(true);

        // when
        boolean exists = reviewRepository.existsByUserIdAndPlaceId(userId, placeId);

        // then
        assertThat(exists).isTrue();
        verify(reviewRepository).existsByUserIdAndPlaceId(userId, placeId);
    }
}
