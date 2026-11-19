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
public interface PlaceRepository extends JpaRepository<Place, UUID>, PlaceRepositoryCustom {

    /**
     * 공공데이터 고유 ID(publicDataId)로 장소 존재 여부 및 조회 (중복 저장 방지용)
     */
    Optional<Place> findByPublicDataId(String publicDataId);

    /**
     * 공공데이터 고유 ID(publicDataId) 존재 여부 확인
     */
    boolean existsByPublicDataId(String publicDataId);

    /**
     * 인기 장소 Top 10 조회 (평점 내림차순, 리뷰 수 내림차순)
     */
    java.util.List<Place> findTop10ByOrderByRatingDescReviewCountDesc();

    /**
     * 최근 등록 장소 Top 10 조회 (최신순)
     */
    java.util.List<Place> findTop10ByOrderByCreatedAtDesc();

    /**
     * TourAPI 반려동물 정책(detailPetTour2)을 아직 조회하지 않은 공공데이터 장소 (TSK-DATA-010).
     * 조회 시각이 없는 장소만 대상이라 배치를 여러 날에 나눠 실행해도 이어서 처리된다(최근 등록 순).
     */
    java.util.List<Place> findByPublicDataIdIsNotNullAndPetPolicySyncedAtIsNullOrderByCreatedAtDesc(
            org.springframework.data.domain.Pageable pageable);
}
