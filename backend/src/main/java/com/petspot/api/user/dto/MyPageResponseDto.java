package com.petspot.api.user.dto;

import com.petspot.api.pet.dto.PetResponseDto;
import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 마이페이지 통합 요약 정보 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPageResponseDto {

    private UUID userId;
    private String email;
    private String nickname;
    private String avatarUrl;
    private UserRole role;
    private PetResponseDto representativePet;
    private long petCount;
    private long favoriteCount;
    private long reviewCount;

    public static MyPageResponseDto of(User user, Pet representativePet, long petCount, long favoriteCount, long reviewCount) {
        return MyPageResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .representativePet(representativePet != null ? PetResponseDto.from(representativePet) : null)
                .petCount(petCount)
                .favoriteCount(favoriteCount)
                .reviewCount(reviewCount)
                .build();
    }
}
