package com.petspot.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 강제/일반 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequestDto {

    @NotBlank(message = "새 비밀번호는 필수 입력값입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$",
            message = "비밀번호는 8자 이상 100자 이하이며 영문, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    @Schema(description = "새 비밀번호 (8~100자, 영문·숫자·특수문자 모두 포함)", example = "NewPass123!")
    private String newPassword;

    @NotBlank(message = "비밀번호 확인은 필수 입력값입니다.")
    @Schema(description = "새 비밀번호 확인 (newPassword와 동일해야 함)", example = "NewPass123!")
    private String confirmPassword;
}
