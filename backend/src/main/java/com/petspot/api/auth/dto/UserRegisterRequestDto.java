package com.petspot.api.auth.dto;

import com.petspot.global.util.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisterRequestDto {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 255, message = "이메일은 최대 255자까지 입력할 수 있습니다.")
    @Schema(description = "이메일 (최대 255자, 중복 불가)", example = "user@petspot.com")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) // TSK-BE-034: 비밀번호 변경과 같은 규칙
    @Schema(description = "비밀번호 (8~100자, 영문·숫자·특수문자 모두 포함)", example = "Password123!")
    private String password;

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하이어야 합니다.")
    @Schema(description = "닉네임 (2~50자, 중복 불가)", example = "댕댕이아빠")
    private String nickname;
}
