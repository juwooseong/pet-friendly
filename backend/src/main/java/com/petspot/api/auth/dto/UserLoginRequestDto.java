package com.petspot.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginRequestDto {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Schema(description = "로그인 이메일", example = "user@petspot.com")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Schema(description = "비밀번호", example = "Password123!")
    private String password;
}
