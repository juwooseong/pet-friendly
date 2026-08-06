package com.petspot.api.place;

import com.petspot.application.place.PlaceQueryService;
import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import com.petspot.global.dto.ApiResponse;
import com.petspot.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 반려동물 동반 장소 검색 REST Controller
 */
@Tag(name = "Place API", description = "반려동물 동반 장소 검색 및 조회 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/places")
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceQueryService placeQueryService;

    @Operation(
            summary = "반려동물 동반 장소 반경 및 복합 조건 검색 (페이징)",
            description = "사용자의 현재 위도/경도 위치 기준 반경 N km 이내, 카테고리, 장소명/주소 키워드, 동반 체중 제한 조건과 Pageable 페이징(page, size)을 적용하여 장소 목록을 검색합니다."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<PlaceSearchResponseDto>>> searchPlaces(
            @ParameterObject @Valid @ModelAttribute PlaceSearchCondition condition,
            @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {

        log.info("GET /api/v1/places/search called with condition: lat={}, lon={}, radiusKm={}, page={}, size={}",
                condition.getLatitude(), condition.getLongitude(), condition.getRadiusKm(),
                pageable.getPageNumber(), pageable.getPageSize());

        PageResponse<PlaceSearchResponseDto> result = placeQueryService.searchPlaces(condition, pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
