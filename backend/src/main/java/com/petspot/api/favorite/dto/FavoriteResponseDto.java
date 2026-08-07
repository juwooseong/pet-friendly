package com.petspot.api.favorite.dto;

import com.petspot.domain.favorite.entity.Favorite;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 즐겨찾기 정보 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteResponseDto {

    private UUID favoriteId;
    private UUID placeId;
    private String placeName;
    private String category;
    private String categoryName;
    private String address;
    private String imageUrl;
    private Double rating;
    private Integer reviewCount;
    private ZonedDateTime createdAt;

    public static FavoriteResponseDto from(Favorite favorite) {
        return FavoriteResponseDto.builder()
                .favoriteId(favorite.getId())
                .placeId(favorite.getPlace().getId())
                .placeName(favorite.getPlace().getName())
                .category(favorite.getPlace().getCategory())
                .categoryName(favorite.getPlace().getCategoryName())
                .address(favorite.getPlace().getAddress())
                .imageUrl(favorite.getPlace().getImageUrl())
                .rating(favorite.getPlace().getRating() != null ? favorite.getPlace().getRating().doubleValue() : 0.0)
                .reviewCount(favorite.getPlace().getReviewCount())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
