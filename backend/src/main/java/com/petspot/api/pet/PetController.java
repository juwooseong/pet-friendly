package com.petspot.api.pet;

import com.petspot.api.pet.dto.PetCreateRequestDto;
import com.petspot.api.pet.dto.PetResponseDto;
import com.petspot.api.pet.dto.PetUpdateRequestDto;
import com.petspot.application.pet.PetService;
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
 * 반려동물 CRUD 관리 REST Controller
 */
@Tag(name = "Pet API", description = "반려동물 프로필 등록/조회/수정/삭제 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @Operation(
            summary = "신규 반려동물 등록",
            description = "현재 로그인한 사용자의 신규 반려동물을 등록합니다. 최초 등록 시 자동으로 대표 반려동물로 지정됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<ApiResponse<PetResponseDto>> registerPet(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PetCreateRequestDto request) {

        log.info("POST /api/v1/pets called by userId: {}", userDetails.getId());

        PetResponseDto result = petService.registerPet(userDetails.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Operation(
            summary = "내 반려동물 목록 조회",
            description = "현재 로그인한 사용자가 등록한 모든 반려동물 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<PetResponseDto>>> getMyPets(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("GET /api/v1/pets called by userId: {}", userDetails.getId());

        List<PetResponseDto> result = petService.getMyPets(userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
            summary = "반려동물 상세 조회",
            description = "반려동물 ID로 단건 정보를 조회합니다. (본인 소유 펫만 접근 가능)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetResponseDto>> getPet(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID petId) {

        log.info("GET /api/v1/pets/{} called by userId: {}", petId, userDetails.getId());

        PetResponseDto result = petService.getPet(userDetails.getId(), petId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
            summary = "반려동물 정보 수정",
            description = "반려동물 정보를 수정합니다. (본인 소유 펫만 수정 가능)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetResponseDto>> updatePet(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID petId,
            @Valid @RequestBody PetUpdateRequestDto request) {

        log.info("PUT /api/v1/pets/{} called by userId: {}", petId, userDetails.getId());

        PetResponseDto result = petService.updatePet(userDetails.getId(), petId, request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
            summary = "반려동물 삭제",
            description = "반려동물을 삭제합니다. 삭제한 펫이 대표 펫이었던 경우 남아있는 펫 중 하나가 대표 펫으로 자동 지정됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{petId}")
    public ResponseEntity<ApiResponse<Void>> deletePet(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID petId) {

        log.info("DELETE /api/v1/pets/{} called by userId: {}", petId, userDetails.getId());

        petService.deletePet(userDetails.getId(), petId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
