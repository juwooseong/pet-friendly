package com.petspot.api.auth.dto;

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
    private String nickname;
}
