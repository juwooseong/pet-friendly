package com.petspot.api.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 장소 리뷰 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCreateRequestDto {

    @NotNull(message = "장소 ID는 필수 입력값입니다.")
    private UUID placeId;

    @NotNull(message = "평점은 필수 입력값입니다.")
    @Min(value = 1, message = "평점은 최소 1점 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 최대 5점 이하이어야 합니다.")
    private Integer rating;

    @NotBlank(message = "리뷰 내용은 필수 입력값입니다.")
    @Size(max = 1000, message = "리뷰 내용은 최대 1000자까지 입력 가능합니다.")
    private String content;

    @Size(max = 1000, message = "이미지 URL은 최대 1000자까지 입력 가능합니다.")
    private String imageUrl;
}
