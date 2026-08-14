package com.petspot.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이메일/닉네임 사용 가능 여부 응답 DTO (회원가입 실시간 중복확인 공용)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponseDto {

    private boolean available;
}
