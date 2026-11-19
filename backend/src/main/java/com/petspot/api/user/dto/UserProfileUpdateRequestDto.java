package com.petspot.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 프로필 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileUpdateRequestDto {

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하이어야 합니다.")
    @Schema(description = "변경할 닉네임 (2~50자)", example = "초코맘")
    private String nickname;

    @Size(max = 1000, message = "프로필 이미지 URL은 최대 1000자까지 입력할 수 있습니다.")
    @Schema(description = "프로필 이미지 URL (최대 1000자, 선택)", example = "https://cdn.petspot.example/avatars/user-001.png")
    private String avatarUrl;
}
