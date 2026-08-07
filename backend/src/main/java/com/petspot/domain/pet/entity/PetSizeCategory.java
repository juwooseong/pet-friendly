package com.petspot.domain.pet.entity;

import java.math.BigDecimal;

/**
 * 반려동물 크기 구분 Enum (체중 기준)
 * SMALL: 10kg 이하
 * MEDIUM: 10kg 초과 20kg 이하
 * LARGE: 20kg 초과
 */
public enum PetSizeCategory {
    SMALL,
    MEDIUM,
    LARGE;

    public static PetSizeCategory fromWeight(BigDecimal weightKg) {
        if (weightKg == null) {
            return SMALL;
        }
        double weight = weightKg.doubleValue();
        if (weight <= 10.0) {
            return SMALL;
        } else if (weight <= 20.0) {
            return MEDIUM;
        } else {
            return LARGE;
        }
    }
}
