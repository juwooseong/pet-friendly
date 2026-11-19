package com.petspot.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 찾기(임시 비밀번호 발급) 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindPasswordRequestDto {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Schema(description = "가입 이메일 (일치하는 계정이 있으면 임시 비밀번호 메일 발송)", example = "user@petspot.com")
    private String email;
}
