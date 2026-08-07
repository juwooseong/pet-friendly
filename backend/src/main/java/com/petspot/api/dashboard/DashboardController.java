package com.petspot.api.dashboard;

import com.petspot.api.dashboard.dto.DashboardResponseDto;
import com.petspot.application.dashboard.DashboardQueryService;
import com.petspot.global.dto.ApiResponse;
import com.petspot.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메인 대시보드(Home) 데이터 조회 REST Controller
 */
@Tag(name = "Dashboard API", description = "메인 홈 화면 대시보드 통합 데이터 조회 API")
@Slf4j
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;

    @Operation(
            summary = "메인 대시보드(Home) 데이터 통합 조회",
            description = "앱 실행 시 필요한 유저 프로필 요약, 대표 반려동물, 즐겨찾기 수, 인기 장소 Top10, 최신 장소 Top10 및 추천 장소를 통합 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponseDto>> getDashboardSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("GET /api/v1/dashboard called by userId: {}", userDetails.getId());

        DashboardResponseDto result = dashboardQueryService.getDashboardSummary(userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
