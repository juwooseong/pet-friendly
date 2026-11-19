package com.petspot.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 로그인 성공 시 JWT 토큰 및 사용자 정보 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginResponseDto {

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzZjJhMWM0ZSJ9.signature")
    private String accessToken;

    @Builder.Default
    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "토큰 만료까지 남은 시간(초)", example = "86400")
    private long expiresIn;

    @Builder.Default
    @Schema(description = "임시 비밀번호 로그인 여부. true면 PATCH /api/v1/auth/password 호출 전 다른 인증 API는 403", example = "false")
    private boolean requiresPasswordChange = false;

    @Schema(description = "로그인 사용자 요약 정보")
    private UserSummaryDto user;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSummaryDto {
        @Schema(description = "회원 ID", example = "3f2a1c4e-8b7d-4e21-9a6f-1c2b3d4e5f60")
        private UUID id;
        @Schema(description = "이메일", example = "user@petspot.com")
        private String email;
        @Schema(description = "닉네임", example = "댕댕이아빠")
        private String nickname;
        @Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://cdn.petspot.example/avatars/user-001.png", nullable = true)
        private String avatarUrl;
        @Schema(description = "권한", example = "USER")
        private UserRole role;

        public static UserSummaryDto from(User user) {
            return UserSummaryDto.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .role(user.getRole())
                    .build();
        }
    }
}
