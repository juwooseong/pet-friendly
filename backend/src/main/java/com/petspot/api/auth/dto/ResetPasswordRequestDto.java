package com.petspot.api.auth.dto;

import com.petspot.global.util.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 요청 DTO (TSK-SEC-003) - 메일 링크의 토큰과 새 비밀번호
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequestDto {

    @NotBlank(message = "재설정 토큰은 필수 입력값입니다.")
    @Size(max = 100, message = "재설정 토큰 형식이 올바르지 않습니다.")
    @Schema(description = "비밀번호 재설정 메일 링크의 token 값")
    private String token;

    @NotBlank(message = "새 비밀번호는 필수 입력값입니다.")
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
    @Schema(description = "새 비밀번호 (8~100자, 영문·숫자·특수문자 모두 포함)", example = "NewPass123!")
    private String newPassword;

    @NotBlank(message = "비밀번호 확인은 필수 입력값입니다.")
    @Schema(description = "새 비밀번호 확인 (newPassword와 동일해야 함)", example = "NewPass123!")
    private String confirmPassword;
}
