package com.petspot.api.auth.dto;

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

    private UUID id;
    private String email;
    private String nickname;
    private UserRole role;
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
