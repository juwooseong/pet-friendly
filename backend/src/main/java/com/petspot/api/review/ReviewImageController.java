package com.petspot.api.review;

import com.petspot.api.review.dto.ReviewImageUploadResponseDto;
import com.petspot.application.review.ReviewImageService;
import com.petspot.global.config.SwaggerConfig;
import com.petspot.global.dto.ApiResponse;
import com.petspot.global.error.ErrorCode;
import com.petspot.global.security.CustomUserDetails;
import com.petspot.global.swagger.ApiErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * 리뷰 사진 올리기·보기 (리뷰당 1장)
 */
@Tag(name = "Review API", description = "장소 리뷰 작성/조회/수정/삭제 API")
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReviewImageController {

    private final ReviewImageService reviewImageService;

    @Operation(
            summary = "리뷰 사진 올리기",
            description = "JPG·PNG 사진 1장(10MB 이하)을 올리고 주소를 받습니다. 받은 imageUrl을 리뷰 등록·수정 요청에 넣습니다. "
                    + "사진은 긴 변 1600px 이하 JPEG로 다시 저장되고(사진 속 위치 정보 등은 지워짐), 목록용 240px 정사각형 썸네일이 함께 만들어집니다.",
            security = @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    )
    @PostMapping(value = "/api/v1/review-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiErrorCodes({ErrorCode.INVALID_REVIEW_IMAGE, ErrorCode.REVIEW_IMAGE_TOO_LARGE})
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<ReviewImageUploadResponseDto>> upload(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile file) {

        log.info("POST /api/v1/review-images called by userId: {}, size: {}", userDetails.getId(), file.getSize());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(reviewImageService.store(file)));
    }

    @Operation(summary = "리뷰 사진 보기", description = "올린 리뷰 사진(원본 또는 _thumb 썸네일)을 내려줍니다. 파일 이름이 바뀌지 않으므로 오래 캐시합니다.")
    @GetMapping("/api/v1/review-images/{fileName}")
    @ApiErrorCodes({ErrorCode.REVIEW_IMAGE_NOT_FOUND})
    public ResponseEntity<Resource> view(@PathVariable String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(reviewImageService.load(fileName));
    }
}
