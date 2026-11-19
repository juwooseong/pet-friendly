package com.petspot.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.petspot.domain.user.entity.OAuthProvider;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import com.petspot.domain.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 마이페이지 사용자 프로필 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDto {

    @Schema(description = "회원 ID", example = "3f2a1c4e-8b7d-4e21-9a6f-1c2b3d4e5f60")
    private UUID id;
    @Schema(description = "이메일", example = "user@petspot.com")
    private String email;
    @Schema(description = "닉네임", example = "초코맘")
    private String nickname;
    @Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://cdn.petspot.example/avatars/user-001.png", nullable = true)
    private String avatarUrl;
    @Schema(description = "권한", example = "USER")
    private UserRole role;
    @Schema(description = "가입 경로", example = "LOCAL")
    private OAuthProvider provider;
    @Schema(description = "계정 상태", example = "ACTIVE")
    private UserStatus status;
    @Schema(description = "가입 일시", example = "2026-11-19T14:30:00+09:00")
    private ZonedDateTime createdAt;

    public static UserProfileResponseDto from(User user) {
        return UserProfileResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .provider(user.getProvider())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
