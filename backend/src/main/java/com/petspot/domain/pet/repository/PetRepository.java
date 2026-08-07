package com.petspot.domain.pet.repository;

import com.petspot.domain.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 반려동물 Spring Data JPA Repository
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, UUID> {

    /**
     * 특정 소유자(User)의 모든 반려동물 목록 조회
     */
    List<Pet> findAllByOwnerId(UUID ownerId);

    /**
     * 특정 소유자의 대표 반려동물 조회
     */
    Optional<Pet> findByOwnerIdAndRepresentativeTrue(UUID ownerId);

    /**
     * 특정 소유자의 대표 반려동물 조회 (편의 래퍼 메서드)
     */
    default Optional<Pet> findRepresentativePet(UUID ownerId) {
        return findByOwnerIdAndRepresentativeTrue(ownerId);
    }

    /**
     * 특정 소유자에게 대표 반려동물이 존재하는지 여부 확인
     */
    boolean existsByOwnerIdAndRepresentativeTrue(UUID ownerId);
}
