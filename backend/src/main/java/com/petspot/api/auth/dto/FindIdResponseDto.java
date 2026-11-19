package com.petspot.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아이디(이메일) 찾기 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindIdResponseDto {

    @Schema(description = "일부 마스킹된 가입 이메일", example = "us**@petspot.com")
    private String maskedEmail;
}
