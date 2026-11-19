package com.petspot.api.pet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.pet.entity.PetGender;
import com.petspot.domain.pet.entity.PetSizeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 반려동물 정보 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetResponseDto {

    @Schema(description = "반려동물 ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    private UUID id;
    @Schema(description = "이름", example = "초코")
    private String name;
    @Schema(description = "견종/품종", example = "푸들")
    private String breed;
    @Schema(description = "성별", example = "FEMALE")
    private PetGender gender;
    @Schema(description = "생년월일 (없으면 null)", example = "2021-03-15", nullable = true)
    private LocalDate birthDate;
    @Schema(description = "체중(kg)", example = "4.2")
    private BigDecimal weightKg;
    @Schema(description = "체중 기반 서버 계산 크기 (SMALL/MEDIUM/LARGE)", example = "SMALL")
    private PetSizeCategory sizeCategory;
    @Schema(description = "중성화 여부", example = "true")
    private boolean neutered;
    @Schema(description = "대표 반려동물 여부", example = "true")
    private boolean representative;
    @Schema(description = "사진 URL (없으면 null)", example = "https://cdn.petspot.example/pets/choco.jpg", nullable = true)
    private String photoUrl;
    @Schema(description = "등록 일시", example = "2026-11-19T14:30:00+09:00")
    private ZonedDateTime createdAt;

    public static PetResponseDto from(Pet pet) {
        return PetResponseDto.builder()
                .id(pet.getId())
                .name(pet.getName())
                .breed(pet.getBreed())
                .gender(pet.getGender())
                .birthDate(pet.getBirthDate())
                .weightKg(pet.getWeightKg())
                .sizeCategory(pet.getSizeCategory())
                .neutered(pet.isNeutered())
                .representative(pet.isRepresentative())
                .photoUrl(pet.getPhotoUrl())
                .createdAt(pet.getCreatedAt())
                .build();
    }
}
