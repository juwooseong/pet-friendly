package com.petspot.api.auth.dto;

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

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn;

    @Builder.Default
    private boolean requiresPasswordChange = false;

    private UserSummaryDto user;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSummaryDto {
        private UUID id;
        private String email;
        private String nickname;
        private String avatarUrl;
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
