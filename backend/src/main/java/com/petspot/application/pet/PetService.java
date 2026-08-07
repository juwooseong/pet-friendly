package com.petspot.application.pet;

import com.petspot.api.pet.dto.PetCreateRequestDto;
import com.petspot.api.pet.dto.PetResponseDto;
import com.petspot.api.pet.dto.PetUpdateRequestDto;
import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.pet.repository.PetRepository;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.PetAccessDeniedException;
import com.petspot.global.error.exception.PetNotFoundException;
import com.petspot.global.error.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 반려동물 CRUD 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;

    /**
     * 반려동물 신규 등록
     * 최초 등록하는 펫인 경우 자동으로 대표 반려동물(representative=true)로 지정
     */
    @Transactional
    public PetResponseDto registerPet(UUID userId, PetCreateRequestDto request) {
        log.info("[PET REGISTER] Registering pet for userId: {}, petName: {}", userId, request.getName());

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (owner.getStatus() != UserStatus.ACTIVE) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다.");
        }

        // 이미 대표 반려동물이 존재하는지 확인
        boolean hasRepresentative = petRepository.existsByOwnerIdAndRepresentativeTrue(userId);
        boolean isFirstPet = !hasRepresentative; // 대표 펫이 없으면 최초 펫으로 간주하여 대표 지정

        Pet pet = Pet.create(
                owner,
                request.getName(),
                request.getBreed(),
                request.getGender(),
                request.getBirthDate(),
                request.getWeightKg(),
                request.getNeutered(),
                isFirstPet,
                request.getPhotoUrl()
        );

        Pet savedPet = petRepository.save(pet);
        log.info("[PET REGISTER SUCCESS] Registered petId: {}, isRepresentative: {}", savedPet.getId(), savedPet.isRepresentative());

        return PetResponseDto.from(savedPet);
    }

    /**
     * 내 모든 반려동물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PetResponseDto> getMyPets(UUID userId) {
        log.info("[PET GET ALL] Fetching all pets for userId: {}", userId);

        List<Pet> pets = petRepository.findAllByOwnerId(userId);
        return pets.stream()
                .map(PetResponseDto::from)
                .toList();
    }

    /**
     * 특정 반려동물 단건 조회 (소유자 권한 검증)
     */
    @Transactional(readOnly = true)
    public PetResponseDto getPet(UUID userId, UUID petId) {
        log.info("[PET GET] Fetching petId: {} for userId: {}", petId, userId);

        Pet pet = findPetAndValidateOwner(userId, petId);
        return PetResponseDto.from(pet);
    }

    /**
     * 반려동물 정보 수정 (소유자 권한 검증 및 도메인 메서드 활용)
     */
    @Transactional
    public PetResponseDto updatePet(UUID userId, UUID petId, PetUpdateRequestDto request) {
        log.info("[PET UPDATE] Updating petId: {} for userId: {}", petId, userId);

        Pet pet = findPetAndValidateOwner(userId, petId);

        // 엔티티 도메인 메서드로 프로필 업데이트 및 체중 기반 사이즈 카테고리 재계산
        pet.updateProfile(
                request.getName(),
                request.getBreed(),
                request.getGender(),
                request.getBirthDate(),
                request.getWeightKg(),
                request.getNeutered(),
                request.getPhotoUrl()
        );

        log.info("[PET UPDATE SUCCESS] Updated petId: {}", petId);
        return PetResponseDto.from(pet);
    }

    /**
     * 반려동물 삭제 (소유자 권한 검증 및 대표 펫 삭제 시 승계 처리)
     */
    @Transactional
    public void deletePet(UUID userId, UUID petId) {
        log.info("[PET DELETE] Deleting petId: {} for userId: {}", petId, userId);

        Pet pet = findPetAndValidateOwner(userId, petId);
        boolean wasRepresentative = pet.isRepresentative();

        petRepository.delete(pet);
        log.info("[PET DELETE SUCCESS] Deleted petId: {}", petId);

        // 삭제한 펫이 대표 펫이었던 경우, 남은 펫 중 하나를 자동으로 대표 펫으로 승계 지정
        if (wasRepresentative) {
            List<Pet> remainingPets = petRepository.findAllByOwnerId(userId);
            if (!remainingPets.isEmpty()) {
                Pet newRepresentative = remainingPets.get(0);
                newRepresentative.changeRepresentative(true);
                log.info("[PET REPRESENTATIVE PROMOTED] Promoted petId: {} as new representative for userId: {}", newRepresentative.getId(), userId);
            }
        }
    }

    /**
     * 대표 반려동물 변경
     * 하나의 트랜잭션 내에서 기존 대표 펫의 representative 상태를 해제하고 targetPet을 대표 펫으로 설정
     */
    @Transactional
    public PetResponseDto setRepresentativePet(UUID userId, UUID petId) {
        log.info("[PET SET REPRESENTATIVE] Setting petId: {} as representative for userId: {}", petId, userId);

        Pet targetPet = findPetAndValidateOwner(userId, petId);

        // 이미 대표 펫인 경우 추가 변경 없이 그대로 반환 (멱등성 보장)
        if (targetPet.isRepresentative()) {
            log.info("[PET SET REPRESENTATIVE] petId: {} is already representative for userId: {}", petId, userId);
            return PetResponseDto.from(targetPet);
        }

        // 기존 대표 반려동물 조회 및 해제
        petRepository.findByOwnerIdAndRepresentativeTrue(userId)
                .ifPresent(currentRep -> {
                    currentRep.changeRepresentative(false);
                    log.info("[PET UNSET REPRESENTATIVE] Unset representative for petId: {}", currentRep.getId());
                });

        // 선택한 펫을 대표 반려동물로 설정
        targetPet.changeRepresentative(true);
        log.info("[PET SET REPRESENTATIVE SUCCESS] petId: {} is now representative for userId: {}", petId, userId);

        return PetResponseDto.from(targetPet);
    }

    /**
     * 반려동물 존재 확인 및 소유자(Owner) 권한 검증 헬퍼 메서드
     */
    private Pet findPetAndValidateOwner(UUID userId, UUID petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException("반려동물을 찾을 수 없습니다."));

        if (!pet.getOwner().getId().equals(userId)) {
            log.warn("[PET ACCESS DENIED] userId: {} attempted to access petId: {} owned by userId: {}", userId, petId, pet.getOwner().getId());
            throw new PetAccessDeniedException("해당 반려동물 정보에 접근할 권한이 없습니다.");
        }

        return pet;
    }
}
