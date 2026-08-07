package com.petspot.api.dashboard.dto;

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
    private String nickname;
    private RepresentativePetDto representativePet;

    // Statistics
    private long favoriteCount;

    // Place Lists (Max 10 per list)
    private List<DashboardPlaceDto> popularPlaces;
    private List<DashboardPlaceDto> recentPlaces;
    private List<DashboardPlaceDto> recommendedPlaces;

    /**
     * 대시보드 표출 전용 장소 간략 요약 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardPlaceDto {
        private UUID id;
        private String name;
        private String category;
        private String categoryName;
        private String imageUrl;
        private BigDecimal rating;
        private Integer reviewCount;
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
