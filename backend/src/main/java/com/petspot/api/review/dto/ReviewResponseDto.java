package com.petspot.api.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.petspot.application.review.ReviewImageService;
import com.petspot.domain.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 리뷰 정보 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto {

    @Schema(description = "리뷰 ID", example = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d")
    private UUID id;
    @Schema(description = "장소 ID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    private UUID placeId;
    @Schema(description = "장소명", example = "멍멍 테라스 카페")
    private String placeName;
    @Schema(description = "작성자 ID", example = "3f2a1c4e-8b7d-4e21-9a6f-1c2b3d4e5f60")
    private UUID userId;
    @Schema(description = "작성자 닉네임", example = "댕댕이아빠")
    private String userNickname;
    @Schema(description = "작성자 프로필 이미지 URL (없으면 null)", example = "https://cdn.petspot.example/avatars/user-001.png", nullable = true)
    private String userAvatarUrl;
    @Schema(description = "평점 (1~5)", example = "4")
    private Integer rating;
    @Schema(description = "리뷰 내용", example = "대형견도 편하게 쉴 수 있는 넓은 테라스가 좋았어요.")
    private String content;
    @Schema(description = "리뷰 이미지 URL (없으면 null)", example = "https://cdn.petspot.example/reviews/r-001.jpg", nullable = true)
    private String imageUrl;
    @Schema(description = "리뷰 목록용 정사각형 썸네일 주소 (사진 없으면 null)", example = "/api/v1/review-images/3f2a1c4e8b7d4e219a6f1c2b3d4e5f60_thumb.jpg", nullable = true)
    private String thumbnailUrl;
    @Schema(description = "작성 일시", example = "2026-11-19T14:30:00+09:00")
    private ZonedDateTime createdAt;

    public static ReviewResponseDto from(Review review) {
        return ReviewResponseDto.builder()
                .id(review.getId())
                .placeId(review.getPlace().getId())
                .placeName(review.getPlace().getName())
                .userId(review.getUser().getId())
                .userNickname(review.getUser().getNickname())
                .userAvatarUrl(review.getUser().getAvatarUrl())
                .rating(review.getRating())
                .content(review.getContent())
                .imageUrl(review.getImageUrl())
                .thumbnailUrl(ReviewImageService.thumbnailUrlOf(review.getImageUrl()))
                .createdAt(review.getCreatedAt())
                .build();
    }
}
