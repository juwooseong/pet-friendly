package com.petspot.api.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(description = "즐겨찾기 ID", example = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d")
    private UUID favoriteId;
    @Schema(description = "장소 ID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    private UUID placeId;
    @Schema(description = "장소명", example = "멍멍 테라스 카페")
    private String placeName;
    @Schema(description = "카테고리 코드", example = "CAFE")
    private String category;
    @Schema(description = "카테고리 표시명", example = "카페")
    private String categoryName;
    @Schema(description = "주소", example = "서울특별시 마포구 와우산로 123")
    private String address;
    @Schema(description = "대표 이미지 URL (없으면 null)", example = "https://cdn.petspot.example/images/place-001.jpg", nullable = true)
    private String imageUrl;
    @Schema(description = "대표 이미지 공공누리 유형 (Type1 출처표시, Type3 출처표시+변경금지 등). 공공데이터 장소만", nullable = true, example = "Type3")
    private String imageCopyrightType;
    @Schema(description = "평균 평점", example = "4.5")
    private Double rating;
    @Schema(description = "리뷰 수", example = "12")
    private Integer reviewCount;
    @Schema(description = "즐겨찾기 등록 일시", example = "2026-11-19T14:30:00+09:00")
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
                .imageCopyrightType(favorite.getPlace().getImageCopyrightType())
                .rating(favorite.getPlace().getRating() != null ? favorite.getPlace().getRating().doubleValue() : 0.0)
                .reviewCount(favorite.getPlace().getReviewCount())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
