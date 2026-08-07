package com.petspot.api.pet.dto;

import com.petspot.domain.pet.entity.PetGender;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 반려동물 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetUpdateRequestDto {

    @NotBlank(message = "반려동물 이름은 필수 입력값입니다.")
    @Size(max = 100, message = "반려동물 이름은 최대 100자까지 입력 가능합니다.")
    private String name;

    @NotBlank(message = "견종/품종은 필수 입력값입니다.")
    @Size(max = 100, message = "견종/품종은 최대 100자까지 입력 가능합니다.")
    private String breed;

    @NotNull(message = "성별은 필수 입력값입니다.")
    private PetGender gender;

    @PastOrPresent(message = "생년월일은 미래 날짜일 수 없습니다.")
    private LocalDate birthDate;

    @NotNull(message = "체중은 필수 입력값입니다.")
    @DecimalMin(value = "0.1", message = "체중은 0.1kg 이상이어야 합니다.")
    private BigDecimal weightKg;

    private Boolean neutered;

    @Size(max = 1000, message = "사진 URL은 최대 1000자까지 입력 가능합니다.")
    private String photoUrl;
}
