package com.petspot.api.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * 장소 1곳의 즐겨찾기 여부 (TSK-BE-030)
 */
@Schema(description = "장소 즐겨찾기 여부")
public record FavoriteStatusResponseDto(
        @Schema(description = "장소 ID", example = "3f2a1c4e-8b7d-4e21-9a6f-1c2b3d4e5f60")
        UUID placeId,
        @Schema(description = "현재 사용자가 즐겨찾기한 장소인지 여부", example = "true")
        boolean favorited
) {
}
