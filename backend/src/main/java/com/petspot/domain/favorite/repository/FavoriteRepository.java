package com.petspot.domain.favorite.repository;

import com.petspot.domain.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 즐겨찾기 Spring Data JPA Repository
 */
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    /**
     * 특정 사용자가 특정 장소를 이미 즐겨찾기했는지 여부 확인
     */
    boolean existsByUserIdAndPlaceId(UUID userId, UUID placeId);

    /**
     * 특정 사용자의 모든 즐겨찾기 목록 조회
     */
    List<Favorite> findAllByUserId(UUID userId);

    /**
     * 특정 사용자의 특정 장소 즐겨찾기 단건 조회
     */
    Optional<Favorite> findByUserIdAndPlaceId(UUID userId, UUID placeId);

    /**
     * 특정 장소가 즐겨찾기 등록된 총 개수 조회
     */
    long countByPlaceId(UUID placeId);

    /**
     * 특정 사용자의 특정 장소 즐겨찾기 해제(삭제)
     */
    void deleteByUserIdAndPlaceId(UUID userId, UUID placeId);
}
