package com.petspot.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 회원가입 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisterResponseDto {

    @Schema(description = "회원 ID", example = "3f2a1c4e-8b7d-4e21-9a6f-1c2b3d4e5f60")
    private UUID id;
    @Schema(description = "이메일", example = "user@petspot.com")
    private String email;
    @Schema(description = "닉네임", example = "댕댕이아빠")
    private String nickname;
    @Schema(description = "권한", example = "USER")
    private UserRole role;
    @Schema(description = "가입 일시", example = "2026-11-19T14:30:00+09:00")
    private ZonedDateTime createdAt;

    public static UserRegisterResponseDto from(User user) {
        return UserRegisterResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
