package com.petspot.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 비밀번호 변경 응답 (TSK-SEC-001).
 * 비밀번호를 바꾸면 이전에 발급한 토큰이 모두 무효화되므로, 변경한 기기가 계속 로그인 상태를 유지하도록 새 토큰을 내려준다.
 */
@Schema(description = "비밀번호 변경 응답 - 이전 토큰은 무효화되며 이후 요청에는 새 토큰을 사용")
public record ChangePasswordResponseDto(
        @Schema(description = "새 JWT Access Token")
        String accessToken,
        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,
        @Schema(description = "만료까지 남은 시간(초)", example = "3600")
        long expiresIn
) {
}
