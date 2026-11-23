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
     * 홈 대시보드 인기 장소 (평점 → 리뷰 수 → 최근 등록 순). 쇼핑은 다른 장소 뒤로 (TSK-FE-022, 검색의 TSK-FE-020과 같은 규칙).
     */
    @org.springframework.data.jpa.repository.Query("""
            select p from Place p
            order by case when p.category = 'SHOPPING' then 1 else 0 end,
                     p.rating desc, p.reviewCount desc, p.createdAt desc, p.id
            """)
    java.util.List<Place> findPopularPlaces(org.springframework.data.domain.Pageable pageable);

    /**
     * 홈 대시보드 최근 등록 장소. 쇼핑은 다른 장소 뒤로 (TSK-FE-022).
     */
    @org.springframework.data.jpa.repository.Query("""
            select p from Place p
            order by case when p.category = 'SHOPPING' then 1 else 0 end, p.createdAt desc, p.id
            """)
    java.util.List<Place> findRecentPlaces(org.springframework.data.domain.Pageable pageable);

    /**
     * TourAPI 반려동물 정책(detailPetTour2)을 아직 조회하지 않은 공공데이터 장소 (TSK-DATA-010).
     * 조회 시각이 없는 장소만 대상이라 배치를 여러 날에 나눠 실행해도 이어서 처리된다.
     *
     * <p>쇼핑이 아닌 장소부터 고른다 (TSK-DATA-011). 쇼핑은 전체의 약 90%지만 정책 문구가 거의 없다(조회한 779곳 중 6곳). 관광지·레포츠·숙박·음식점 등을
     * 먼저 조회해야 하루 호출 한도 안에서 쓸모 있는 정책이 빨리 채워진다. 같은 그룹 안에서는 최근 등록 순.
     */
    @org.springframework.data.jpa.repository.Query("""
            select p from Place p
            where p.publicDataId is not null and p.petPolicySyncedAt is null
            order by case when p.category = 'SHOPPING' then 1 else 0 end, p.createdAt desc
            """)
    java.util.List<Place> findPendingPetPolicySync(org.springframework.data.domain.Pageable pageable);
}
