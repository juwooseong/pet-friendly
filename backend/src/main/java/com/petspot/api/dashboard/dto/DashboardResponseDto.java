package com.petspot.api.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.petspot.api.user.dto.MyPageResponseDto.RepresentativePetDto;
import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 메인 대시보드(Home) 통합 요약 정보 응답 DTO (Immutable)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponseDto {

    // User Summary
    @Schema(description = "닉네임", example = "댕댕이아빠")
    private String nickname;
    @Schema(description = "대표 반려동물 (없으면 null)", nullable = true)
    private RepresentativePetDto representativePet;

    // Statistics
    @Schema(description = "즐겨찾기 수", example = "3")
    private long favoriteCount;

    // Place Lists (Max 10 per list)
    @Schema(description = "인기 장소 목록 (최대 10개)")
    private List<DashboardPlaceDto> popularPlaces;
    @Schema(description = "최근 등록 장소 목록 (최대 10개)")
    private List<DashboardPlaceDto> recentPlaces;
    @Schema(description = "추천 장소 목록 (최대 10개)")
    private List<DashboardPlaceDto> recommendedPlaces;

    /**
     * 대시보드 표출 전용 장소 간략 요약 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardPlaceDto {
        @Schema(description = "장소 ID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        private UUID id;
        @Schema(description = "장소명", example = "멍멍 테라스 카페")
        private String name;
        @Schema(description = "카테고리 코드", example = "CAFE")
        private String category;
        @Schema(description = "카테고리 표시명", example = "카페")
        private String categoryName;
        @Schema(description = "대표 이미지 URL (없으면 null)", example = "https://cdn.petspot.example/images/place-001.jpg", nullable = true)
        private String imageUrl;
        @Schema(description = "평균 평점", example = "4.5")
        private BigDecimal rating;
        @Schema(description = "리뷰 수", example = "12")
        private Integer reviewCount;
        @Schema(description = "거리(km, 위치 미제공 시 null)", example = "1.2", nullable = true)
        private Double distance;

        public static DashboardPlaceDto from(Place place) {
            if (place == null) {
                return null;
            }
            return DashboardPlaceDto.builder()
                    .id(place.getId())
                    .name(place.getName())
                    .category(place.getCategory())
                    .categoryName(place.getCategoryName())
                    .imageUrl(place.getImageUrl())
                    .rating(place.getRating())
                    .reviewCount(place.getReviewCount())
                    .build();
        }
    }

    public static DashboardResponseDto of(User user, Pet representativePet, long favoriteCount,
                                         List<Place> popularPlaces, List<Place> recentPlaces, List<Place> recommendedPlaces) {
        return DashboardResponseDto.builder()
                .nickname(user.getNickname())
                .representativePet(RepresentativePetDto.from(representativePet))
                .favoriteCount(favoriteCount)
                .popularPlaces(popularPlaces.stream().map(DashboardPlaceDto::from).toList())
                .recentPlaces(recentPlaces.stream().map(DashboardPlaceDto::from).toList())
                .recommendedPlaces(recommendedPlaces.stream().map(DashboardPlaceDto::from).toList())
                .build();
    }
}
