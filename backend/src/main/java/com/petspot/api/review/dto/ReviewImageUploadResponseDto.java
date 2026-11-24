package com.petspot.api.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리뷰 사진 업로드 응답. imageUrl을 리뷰 등록·수정 요청의 imageUrl로 보낸다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewImageUploadResponseDto {

    @Schema(description = "사진 주소 (리뷰 등록 시 imageUrl로 사용)", example = "/api/v1/review-images/3f2a1c4e8b7d4e219a6f1c2b3d4e5f60.jpg")
    private String imageUrl;

    @Schema(description = "목록용 정사각형 썸네일 주소 (240px)", example = "/api/v1/review-images/3f2a1c4e8b7d4e219a6f1c2b3d4e5f60_thumb.jpg")
    private String thumbnailUrl;
}
