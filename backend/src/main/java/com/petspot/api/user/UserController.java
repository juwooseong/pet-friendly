package com.petspot.api.user;

import com.petspot.api.user.dto.MyPageResponseDto;
import com.petspot.api.user.dto.UserProfileResponseDto;
import com.petspot.api.user.dto.UserProfileUpdateRequestDto;
import com.petspot.application.user.MyPageQueryService;
import com.petspot.application.user.UserProfileService;
import com.petspot.global.dto.ApiResponse;
import com.petspot.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 마이페이지 사용자 프로필 및 요약 정보 관리 REST Controller
 */
@Tag(name = "User API", description = "마이페이지 유저 프로필 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final MyPageQueryService myPageQueryService;

    @Operation(
            summary = "현재 로그인 사용자 마이페이지 요약 정보 조회",
            description = "JWT 인증을 통해 현재 로그인한 사용자의 프로필, 대표 반려동물, 등록 펫 수, 즐겨찾기 수, 작성 리뷰 수를 통합 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyPageResponseDto>> getMyPageSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("GET /api/v1/users/me called by userId: {}", userDetails.getId());

        MyPageResponseDto result = myPageQueryService.getMyPageSummary(userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
            summary = "현재 로그인 사용자 프로필 수정",
            description = "JWT 인증을 통해 현재 로그인한 사용자 본인의 닉네임 및 프로필 이미지를 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserProfileUpdateRequestDto request) {

        log.info("PUT /api/v1/users/me called by userId: {}", userDetails.getId());

        UserProfileResponseDto result = userProfileService.updateProfile(userDetails.getId(), request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
