package com.petspot.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아이디(이메일) 찾기 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindIdRequestDto {

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Schema(description = "가입 시 등록한 닉네임", example = "댕댕이아빠")
    private String nickname;
}
