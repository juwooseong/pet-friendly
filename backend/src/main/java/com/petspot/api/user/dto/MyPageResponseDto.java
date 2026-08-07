package com.petspot.api.user.dto;

import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.pet.entity.PetSizeCategory;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 마이페이지 통합 요약 정보 응답 DTO (Immutable)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPageResponseDto {

    // User Profile Information
    private UUID userId;
    private String email;
    private String nickname;
    private String avatarUrl;
    private UserRole role;

    // Representative Pet Information
    private RepresentativePetDto representativePet;

    // User Statistics
    private long petCount;
    private long favoriteCount;
    private long reviewCount;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RepresentativePetDto {
        private UUID petId;
        private String petName;
        private String breed;
        private BigDecimal weightKg;
        private PetSizeCategory sizeCategory;

        public static RepresentativePetDto from(Pet pet) {
            if (pet == null) {
                return null;
            }
            return RepresentativePetDto.builder()
                    .petId(pet.getId())
                    .petName(pet.getName())
                    .breed(pet.getBreed())
                    .weightKg(pet.getWeightKg())
                    .sizeCategory(pet.getSizeCategory())
                    .build();
        }
    }

    public static MyPageResponseDto of(User user, Pet representativePet, long petCount, long favoriteCount, long reviewCount) {
        return MyPageResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .representativePet(RepresentativePetDto.from(representativePet))
                .petCount(petCount)
                .favoriteCount(favoriteCount)
                .reviewCount(reviewCount)
                .build();
    }
}
