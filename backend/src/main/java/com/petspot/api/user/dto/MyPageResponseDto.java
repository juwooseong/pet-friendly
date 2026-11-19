package com.petspot.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "회원 ID", example = "3f2a1c4e-8b7d-4e21-9a6f-1c2b3d4e5f60")
    private UUID userId;
    @Schema(description = "이메일", example = "user@petspot.com")
    private String email;
    @Schema(description = "닉네임", example = "댕댕이아빠")
    private String nickname;
    @Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://cdn.petspot.example/avatars/user-001.png", nullable = true)
    private String avatarUrl;
    @Schema(description = "권한", example = "USER")
    private UserRole role;

    // Representative Pet Information
    @Schema(description = "대표 반려동물 (없으면 null)", nullable = true)
    private RepresentativePetDto representativePet;

    // User Statistics
    @Schema(description = "등록한 반려동물 수", example = "2")
    private long petCount;
    @Schema(description = "즐겨찾기 수", example = "3")
    private long favoriteCount;
    @Schema(description = "작성한 리뷰 수", example = "5")
    private long reviewCount;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RepresentativePetDto {
        @Schema(description = "반려동물 ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        private UUID petId;
        @Schema(description = "이름", example = "초코")
        private String petName;
        @Schema(description = "견종/품종", example = "푸들")
        private String breed;
        @Schema(description = "체중(kg)", example = "4.2")
        private BigDecimal weightKg;
        @Schema(description = "크기 (SMALL/MEDIUM/LARGE)", example = "SMALL")
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
