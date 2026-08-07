package com.petspot.api.favorite;

import com.petspot.api.favorite.dto.FavoriteResponseDto;
import com.petspot.application.favorite.FavoriteService;
import com.petspot.global.dto.ApiResponse;
import com.petspot.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 즐겨찾기(Favorite) CUD 및 조회 REST Controller
 */
@Tag(name = "Favorite API", description = "장소 즐겨찾기 등록/해제 및 내 즐겨찾기 목록 API")
@Slf4j
@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(
            summary = "즐겨찾기 추가",
            description = "특정 장소를 내 즐겨찾기에 등록합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/v1/favorites/{placeId}")
    public ResponseEntity<ApiResponse<FavoriteResponseDto>> addFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID placeId) {

        log.info("POST /api/v1/favorites/{} called by userId: {}", placeId, userDetails.getId());

        FavoriteResponseDto result = favoriteService.addFavorite(userDetails.getId(), placeId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Operation(
            summary = "즐겨찾기 해제(삭제)",
            description = "내 즐겨찾기에서 특정 장소를 해제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/api/v1/favorites/{placeId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID placeId) {

        log.info("DELETE /api/v1/favorites/{} called by userId: {}", placeId, userDetails.getId());

        favoriteService.removeFavorite(userDetails.getId(), placeId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "내 즐겨찾기 목록 조회",
            description = "현재 로그인한 사용자가 즐겨찾기에 등록한 장소 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/api/v1/users/me/favorites")
    public ResponseEntity<ApiResponse<List<FavoriteResponseDto>>> getMyFavorites(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("GET /api/v1/users/me/favorites called by userId: {}", userDetails.getId());

        List<FavoriteResponseDto> result = favoriteService.getMyFavorites(userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
