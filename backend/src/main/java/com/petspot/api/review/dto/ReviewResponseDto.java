package com.petspot.api.review.dto;

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

    private UUID id;
    private UUID placeId;
    private String placeName;
    private UUID userId;
    private String userNickname;
    private String userAvatarUrl;
    private Integer rating;
    private String content;
    private String imageUrl;
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
                .createdAt(review.getCreatedAt())
                .build();
    }
}
