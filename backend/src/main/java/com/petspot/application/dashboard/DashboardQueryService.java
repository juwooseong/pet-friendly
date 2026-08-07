package com.petspot.application.dashboard;

import com.petspot.api.dashboard.dto.DashboardResponseDto;
import com.petspot.domain.favorite.repository.FavoriteRepository;
import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.pet.repository.PetRepository;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 메인 대시보드(Home) 통합 요약 정보 조회 전담 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final FavoriteRepository favoriteRepository;
    private final PlaceRepository placeRepository;

    /**
     * 메인 대시보드 홈 화면 데이터 통합 조회
     * (사용자 프로필 요약, 대표 반려동물, 즐겨찾기 수, 인기 장소 Top10, 최신 등록 장소 Top10, 추천 장소 Top10)
     */
    public DashboardResponseDto getDashboardSummary(UUID userId) {
        long startTime = System.currentTimeMillis();
        log.info("[DASHBOARD GET START] Requesting dashboard summary for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[DASHBOARD GET FAILED] User not found with id: {}", userId);
                    return new UserNotFoundException("사용자를 찾을 수 없습니다.");
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("[DASHBOARD GET FAILED] User is inactive: status={}, userId={}", user.getStatus(), userId);
            throw new UserNotFoundException("사용자를 찾을 수 없습니다.");
        }

        // 1. 대표 반려동물 및 즐겨찾기 수 조회
        Optional<Pet> representativePetOpt = petRepository.findRepresentativePet(userId);
        long favoriteCount = favoriteRepository.countByUserId(userId);

        // 2. 인기 장소 및 최신 장소 Top 10 조회 (최대 10건 제한)
        List<Place> popularPlaces = placeRepository.findTop10ByOrderByRatingDescReviewCountDesc();
        List<Place> recentPlaces = placeRepository.findTop10ByOrderByCreatedAtDesc();

        // 3. 추천 장소 (Sprint 2에서는 인기 장소 기반 제공, Sprint 4에서 AI 추천 알고리즘으로 확장)
        List<Place> recommendedPlaces = popularPlaces;

        log.debug("[DASHBOARD DEBUG] Popular places fetched count: {}", popularPlaces.size());
        log.debug("[DASHBOARD DEBUG] Recent places fetched count: {}", recentPlaces.size());
        log.debug("[DASHBOARD DEBUG] Recommended places fetched count: {}", recommendedPlaces.size());

        DashboardResponseDto result = DashboardResponseDto.of(
                user, representativePetOpt.orElse(null), favoriteCount, popularPlaces, recentPlaces, recommendedPlaces);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[DASHBOARD GET COMPLETED] userId: {}, executionTime: {} ms", userId, elapsedTime);

        return result;
    }
}
