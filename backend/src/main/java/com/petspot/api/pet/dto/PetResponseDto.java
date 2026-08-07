package com.petspot.api.pet.dto;

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

    private UUID id;
    private String name;
    private String breed;
    private PetGender gender;
    private LocalDate birthDate;
    private BigDecimal weightKg;
    private PetSizeCategory sizeCategory;
    private boolean neutered;
    private boolean representative;
    private String photoUrl;
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
