package com.petspot.domain.pet.repository;

import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.pet.entity.PetGender;
import com.petspot.domain.pet.entity.PetSizeCategory;
import com.petspot.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class PetRepositoryTest {

    @Mock
    private PetRepository petRepository;

    private User mockOwner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockOwner = User.register("owner@petspot.com", "pwd_hash", "보호자");
    }

    @Test
    @DisplayName("Pet Entity 정적 팩토리 메서드로 객체 생성 및 기본값/체중 사이즈 자동 계산 검증")
    void petEntity_Create_Success() {
        // given
        Pet pet = Pet.create(
                mockOwner,
                "초코",
                "골든 리트리버",
                PetGender.MALE,
                LocalDate.of(2022, 5, 10),
                new BigDecimal("25.5"), // 20kg 초과 -> LARGE
                true,
                true,
                "https://example.com/choco.jpg"
        );

        // then
        assertThat(pet.getName()).isEqualTo("초코");
        assertThat(pet.getBreed()).isEqualTo("골든 리트리버");
        assertThat(pet.getGender()).isEqualTo(PetGender.MALE);
        assertThat(pet.getWeightKg()).isEqualTo(new BigDecimal("25.5"));
        assertThat(pet.getSizeCategory()).isEqualTo(PetSizeCategory.LARGE);
        assertThat(pet.isNeutered()).isTrue();
        assertThat(pet.isRepresentative()).isTrue();
    }

    @Test
    @DisplayName("Pet 도메인 메서드(updateProfile, changeWeight, changeRepresentative) 기능 검증")
    void petEntity_DomainMethods_Success() {
        // given
        Pet pet = Pet.create(
                mockOwner,
                "뽀삐",
                "말티즈",
                PetGender.FEMALE,
                LocalDate.of(2023, 1, 1),
                new BigDecimal("4.5"), // SMALL
                false,
                false,
                null
        );

        // when 1: 체중 변경 (4.5kg -> 12.0kg -> MEDIUM)
        pet.changeWeight(new BigDecimal("12.0"));
        assertThat(pet.getWeightKg()).isEqualTo(new BigDecimal("12.0"));
        assertThat(pet.getSizeCategory()).isEqualTo(PetSizeCategory.MEDIUM);

        // when 2: 대표 반려동물 지정
        pet.changeRepresentative(true);
        assertThat(pet.isRepresentative()).isTrue();

        // when 3: 프로필 업데이트
        pet.updateProfile("뽀삐2", "말티푸", PetGender.FEMALE, null, new BigDecimal("8.0"), true, "https://example.com/bbo.png");
        assertThat(pet.getName()).isEqualTo("뽀삐2");
        assertThat(pet.getBreed()).isEqualTo("말티푸");
        assertThat(pet.getSizeCategory()).isEqualTo(PetSizeCategory.SMALL);
        assertThat(pet.isNeutered()).isTrue();
    }

    @Test
    @DisplayName("소유자(User) ID로 반려동물 목록 조회 (findAllByOwnerId) Mock 테스트")
    void findAllByOwnerId_Success() {
        // given
        UUID ownerId = mockOwner.getId();
        Pet pet1 = Pet.create(mockOwner, "초코", "푸들", PetGender.MALE, null, new BigDecimal("5.0"), true, true, null);
        Pet pet2 = Pet.create(mockOwner, "쿠키", "포메라니안", PetGender.FEMALE, null, new BigDecimal("3.0"), false, false, null);

        given(petRepository.findAllByOwnerId(ownerId)).willReturn(List.of(pet1, pet2));

        // when
        List<Pet> pets = petRepository.findAllByOwnerId(ownerId);

        // then
        assertThat(pets).hasSize(2);
        assertThat(pets.get(0).getName()).isEqualTo("초코");
        assertThat(pets.get(1).getName()).isEqualTo("쿠키");
        verify(petRepository).findAllByOwnerId(ownerId);
    }

    @Test
    @DisplayName("소유자의 대표 반려동물 조회 (findRepresentativePet) Mock 테스트")
    void findRepresentativePet_Success() {
        // given
        UUID ownerId = mockOwner.getId();
        Pet representativePet = Pet.create(mockOwner, "초코", "푸들", PetGender.MALE, null, new BigDecimal("5.0"), true, true, null);

        given(petRepository.findByOwnerIdAndRepresentativeTrue(ownerId)).willReturn(Optional.of(representativePet));
        given(petRepository.findRepresentativePet(ownerId)).willReturn(Optional.of(representativePet));

        // when
        Optional<Pet> petOpt = petRepository.findRepresentativePet(ownerId);

        // then
        assertThat(petOpt).isPresent();
        assertThat(petOpt.get().isRepresentative()).isTrue();
        assertThat(petOpt.get().getName()).isEqualTo("초코");
    }

    @Test
    @DisplayName("소유자의 대표 반려동물 존재 여부 확인 (existsByOwnerIdAndRepresentativeTrue) Mock 테스트")
    void existsByOwnerIdAndRepresentativeTrue_Success() {
        // given
        UUID ownerId = mockOwner.getId();
        given(petRepository.existsByOwnerIdAndRepresentativeTrue(ownerId)).willReturn(true);

        // when
        boolean exists = petRepository.existsByOwnerIdAndRepresentativeTrue(ownerId);

        // then
        assertThat(exists).isTrue();
        verify(petRepository).existsByOwnerIdAndRepresentativeTrue(ownerId);
    }
}
