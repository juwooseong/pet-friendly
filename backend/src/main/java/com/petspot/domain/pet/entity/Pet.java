package com.petspot.domain.pet.entity;

import com.petspot.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 반려동물 JPA 엔티티 (User Aggregate 종속)
 */
@Entity
@Table(name = "pets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "species", nullable = false, length = 20)
    private String species;

    @Column(name = "breed", nullable = false, length = 100)
    private String breed;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private PetGender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "weight_kg", nullable = false, precision = 4, scale = 1)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "size_category", nullable = false, length = 20)
    private PetSizeCategory sizeCategory;

    @Column(name = "is_neutered", nullable = false)
    private boolean neutered;

    @Column(name = "is_representative", nullable = false)
    private boolean representative;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Builder(access = AccessLevel.PROTECTED)
    protected Pet(UUID id, User owner, String name, String species, String breed,
                 PetGender gender, LocalDate birthDate, BigDecimal weightKg,
                 PetSizeCategory sizeCategory, Boolean neutered, Boolean representative,
                 String photoUrl) {
        this.id = id != null ? id : UUID.randomUUID();
        this.owner = owner;
        this.name = name;
        this.species = species != null ? species : "DOG";
        this.breed = breed;
        this.gender = gender != null ? gender : PetGender.MALE;
        this.birthDate = birthDate;
        this.weightKg = weightKg;
        this.sizeCategory = sizeCategory != null ? sizeCategory : PetSizeCategory.fromWeight(weightKg);
        this.neutered = neutered != null ? neutered : false;
        this.representative = representative != null ? representative : false;
        this.photoUrl = photoUrl;
    }

    /**
     * 반려동물 신규 등록 정적 팩토리 메서드
     */
    public static Pet create(User owner, String name, String breed, PetGender gender,
                            LocalDate birthDate, BigDecimal weightKg, Boolean neutered,
                            boolean representative, String photoUrl) {
        return Pet.builder()
                .owner(owner)
                .name(name)
                .species("DOG")
                .breed(breed)
                .gender(gender)
                .birthDate(birthDate)
                .weightKg(weightKg)
                .sizeCategory(PetSizeCategory.fromWeight(weightKg))
                .neutered(neutered)
                .representative(representative)
                .photoUrl(photoUrl)
                .build();
    }

    /**
     * 프로필 정보 수정 (도메인 메서드)
     */
    public void updateProfile(String name, String breed, PetGender gender,
                              LocalDate birthDate, BigDecimal weightKg,
                              Boolean neutered, String photoUrl) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (breed != null && !breed.isBlank()) {
            this.breed = breed;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
        if (weightKg != null) {
            this.weightKg = weightKg;
            this.sizeCategory = PetSizeCategory.fromWeight(weightKg);
        }
        if (neutered != null) {
            this.neutered = neutered;
        }
        if (photoUrl != null) {
            this.photoUrl = photoUrl;
        }
    }

    /**
     * 대표 반려동물 지정 여부 변경 (도메인 메서드)
     */
    public void changeRepresentative(boolean representative) {
        this.representative = representative;
    }

    /**
     * 체중 변경 및 사이즈 카테고리 재계산 (도메인 메서드)
     */
    public void changeWeight(BigDecimal newWeightKg) {
        if (newWeightKg != null) {
            this.weightKg = newWeightKg;
            this.sizeCategory = PetSizeCategory.fromWeight(newWeightKg);
        }
    }
}
