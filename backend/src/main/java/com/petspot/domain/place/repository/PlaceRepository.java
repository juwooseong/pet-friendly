package com.petspot.domain.place.repository;

import com.petspot.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 반려동물 동반 장소 Spring Data JPA Repository
 */
@Repository
public interface PlaceRepository extends JpaRepository<Place, UUID> {

    /**
     * 공공데이터 고유 ID(publicDataId)로 장소 존재 여부 및 조회 (중복 저장 방지용)
     */
    Optional<Place> findByPublicDataId(String publicDataId);

    /**
     * 공공데이터 고유 ID(publicDataId) 존재 여부 확인
     */
    boolean existsByPublicDataId(String publicDataId);
}
