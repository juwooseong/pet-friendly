package com.petspot.api.review;

import com.petspot.api.review.dto.ReviewCreateRequestDto;
import com.petspot.api.review.dto.ReviewResponseDto;
import com.petspot.api.review.dto.ReviewUpdateRequestDto;
import com.petspot.application.review.ReviewService;
import com.petspot.global.dto.ApiResponse;
import com.petspot.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 장소 리뷰 CUD 및 조회 REST Controller
 */
@Tag(name = "Review API", description = "장소 리뷰 작성/조회/수정/삭제 API")
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "신규 장소 리뷰 작성",
            description = "방문한 장소에 대해 신규 리뷰를 작성합니다. (장소당 1회만 작성 가능)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/reviews")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewCreateRequestDto request) {

        log.info("POST /api/v1/reviews called by userId: {}, placeId: {}", userDetails.getId(), request.getPlaceId());

        ReviewResponseDto result = reviewService.createReview(userDetails.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Operation(
            summary = "특정 장소의 리뷰 목록 조회",
            description = "장소 ID로 해당 장소에 작성된 모든 삭제되지 않은 리뷰 목록을 조회합니다."
    )
    @GetMapping("/api/v1/places/{placeId}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponseDto>>> getPlaceReviews(
            @PathVariable UUID placeId) {

        log.info("GET /api/v1/places/{}/reviews called", placeId);

        List<ReviewResponseDto> result = reviewService.getPlaceReviews(placeId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
            summary = "내가 작성한 리뷰 목록 조회",
            description = "현재 로그인한 사용자가 작성한 모든 삭제되지 않은 리뷰 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/users/me/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponseDto>>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("GET /api/v1/users/me/reviews called by userId: {}", userDetails.getId());

        List<ReviewResponseDto> result = reviewService.getMyReviews(userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
            summary = "리뷰 수정",
            description = "자신이 작성한 리뷰의 평점, 내용, 이미지를 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/api/v1/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> updateReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewUpdateRequestDto request) {

        log.info("PUT /api/v1/reviews/{} called by userId: {}", reviewId, userDetails.getId());

        ReviewResponseDto result = reviewService.updateReview(userDetails.getId(), reviewId, request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
            summary = "리뷰 삭제",
            description = "자신이 작성한 리뷰를 삭제합니다. (Soft Delete 처리)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/api/v1/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID reviewId) {

        log.info("DELETE /api/v1/reviews/{} called by userId: {}", reviewId, userDetails.getId());

        reviewService.deleteReview(userDetails.getId(), reviewId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
