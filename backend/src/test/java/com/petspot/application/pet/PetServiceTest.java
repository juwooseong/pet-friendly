package com.petspot.application.pet;

import com.petspot.api.pet.dto.PetCreateRequestDto;
import com.petspot.api.pet.dto.PetResponseDto;
import com.petspot.api.pet.dto.PetUpdateRequestDto;
import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.pet.entity.PetGender;
import com.petspot.domain.pet.entity.PetSizeCategory;
import com.petspot.domain.pet.repository.PetRepository;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.PetAccessDeniedException;
import com.petspot.global.error.exception.PetNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PetService petService;

    private User mockUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockUser = User.register("owner@petspot.com", "hash", "보호자");
        otherUser = User.register("other@petspot.com", "hash", "다른보호자");
    }

    @Test
    @DisplayName("최초 반려동물 등록 시 representative=true로 자동 지정")
    void registerPet_FirstPet_SetsRepresentativeTrue() {
        // given
        UUID userId = mockUser.getId();
        PetCreateRequestDto request = PetCreateRequestDto.builder()
                .name("초코")
                .breed("푸들")
                .gender(PetGender.MALE)
                .birthDate(LocalDate.of(2022, 1, 1))
                .weightKg(new BigDecimal("5.5"))
                .neutered(true)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(petRepository.existsByOwnerIdAndRepresentativeTrue(userId)).willReturn(false);

        Pet mockSavedPet = Pet.create(mockUser, "초코", "푸들", PetGender.MALE, LocalDate.of(2022, 1, 1), new BigDecimal("5.5"), true, true, null);
        given(petRepository.save(any(Pet.class))).willReturn(mockSavedPet);

        // when
        PetResponseDto response = petService.registerPet(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("초코");
        assertThat(response.isRepresentative()).isTrue();
        assertThat(response.getSizeCategory()).isEqualTo(PetSizeCategory.SMALL);

        verify(petRepository).existsByOwnerIdAndRepresentativeTrue(userId);
        verify(petRepository).save(any(Pet.class));
    }

    @Test
    @DisplayName("이미 대표 펫이 존재하는 상태에서 두 번째 펫 등록 시 representative=false로 지정")
    void registerPet_SecondPet_SetsRepresentativeFalse() {
        // given
        UUID userId = mockUser.getId();
        PetCreateRequestDto request = PetCreateRequestDto.builder()
                .name("쿠키")
                .breed("말티즈")
                .gender(PetGender.FEMALE)
                .weightKg(new BigDecimal("3.0"))
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(petRepository.existsByOwnerIdAndRepresentativeTrue(userId)).willReturn(true);

        Pet mockSavedPet = Pet.create(mockUser, "쿠키", "말티즈", PetGender.FEMALE, null, new BigDecimal("3.0"), false, false, null);
        given(petRepository.save(any(Pet.class))).willReturn(mockSavedPet);

        // when
        PetResponseDto response = petService.registerPet(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isRepresentative()).isFalse();
    }

    @Test
    @DisplayName("내 모든 반려동물 목록 조회 성공")
    void getMyPets_Success() {
        // given
        UUID userId = mockUser.getId();
        Pet pet1 = Pet.create(mockUser, "초코", "푸들", PetGender.MALE, null, new BigDecimal("5.0"), true, true, null);
        Pet pet2 = Pet.create(mockUser, "쿠키", "말티즈", PetGender.FEMALE, null, new BigDecimal("3.0"), false, false, null);

        given(petRepository.findAllByOwnerId(userId)).willReturn(List.of(pet1, pet2));

        // when
        List<PetResponseDto> pets = petService.getMyPets(userId);

        // then
        assertThat(pets).hasSize(2);
        assertThat(pets.get(0).getName()).isEqualTo("초코");
        assertThat(pets.get(1).getName()).isEqualTo("쿠키");
        verify(petRepository).findAllByOwnerId(userId);
    }

    @Test
    @DisplayName("타인의 반려동물 단건 조회 시 PetAccessDeniedException 예외 발생")
    void getPet_OtherOwner_ThrowsPetAccessDeniedException() {
        // given
        UUID userId = mockUser.getId();
        Pet otherPet = Pet.create(otherUser, "남의개", "시바견", PetGender.MALE, null, new BigDecimal("10.0"), true, true, null);
        UUID otherPetId = otherPet.getId();

        given(petRepository.findById(otherPetId)).willReturn(Optional.of(otherPet));

        // when & then
        assertThatThrownBy(() -> petService.getPet(userId, otherPetId))
                .isInstanceOf(PetAccessDeniedException.class)
                .hasMessage("해당 반려동물 정보에 접근할 권한이 없습니다.");
    }

    @Test
    @DisplayName("대표 반려동물 삭제 시 남아있는 펫 중 첫 번째 펫을 자동으로 대표 펫으로 승계")
    void deletePet_RepresentativePet_PromotesNextPet() {
        // given
        UUID userId = mockUser.getId();
        Pet repPet = Pet.create(mockUser, "대표개", "골든리트리버", PetGender.MALE, null, new BigDecimal("25.0"), true, true, null);
        Pet secondPet = Pet.create(mockUser, "서브개", "시츄", PetGender.FEMALE, null, new BigDecimal("4.0"), false, false, null);

        given(petRepository.findById(repPet.getId())).willReturn(Optional.of(repPet));
        given(petRepository.findAllByOwnerId(userId)).willReturn(List.of(secondPet));

        // when
        petService.deletePet(userId, repPet.getId());

        // then
        verify(petRepository).delete(repPet);
        assertThat(secondPet.isRepresentative()).isTrue();
    }

    @Test
    @DisplayName("대표 반려동물 변경 성공 - 기존 대표 펫 해제 및 신규 펫 대표 지정")
    void setRepresentativePet_Success() {
        // given
        UUID userId = mockUser.getId();
        Pet oldRepPet = Pet.create(mockUser, "기존대표", "푸들", PetGender.MALE, null, new BigDecimal("5.0"), true, true, null);
        Pet newRepPet = Pet.create(mockUser, "새대표", "말티즈", PetGender.FEMALE, null, new BigDecimal("3.0"), false, false, null);

        given(petRepository.findById(newRepPet.getId())).willReturn(Optional.of(newRepPet));
        given(petRepository.findByOwnerIdAndRepresentativeTrue(userId)).willReturn(Optional.of(oldRepPet));

        // when
        PetResponseDto response = petService.setRepresentativePet(userId, newRepPet.getId());

        // then
        assertThat(oldRepPet.isRepresentative()).isFalse();
        assertThat(newRepPet.isRepresentative()).isTrue();
        assertThat(response.isRepresentative()).isTrue();
        assertThat(response.getName()).isEqualTo("새대표");
    }
}
