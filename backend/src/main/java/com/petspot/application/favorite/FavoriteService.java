package com.petspot.application.favorite;

import com.petspot.api.favorite.dto.FavoriteResponseDto;
import com.petspot.domain.favorite.entity.Favorite;
import com.petspot.domain.favorite.repository.FavoriteRepository;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.DuplicateFavoriteException;
import com.petspot.global.error.exception.FavoriteNotFoundException;
import com.petspot.global.error.exception.PlaceNotFoundException;
import com.petspot.global.error.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 즐겨찾기(Favorite) CUD 및 조회 애플리케이션 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    /**
     * 특정 장소를 내 즐겨찾기에 등록
     */
    @Transactional
    public FavoriteResponseDto addFavorite(UUID userId, UUID placeId) {
        long startTime = System.currentTimeMillis();
        log.info("[FAVORITE ADD START] userId: {}, placeId: {}", userId, placeId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserNotFoundException();
        }

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new PlaceNotFoundException());

        if (favoriteRepository.existsByUserIdAndPlaceId(userId, placeId)) {
            log.warn("[FAVORITE ADD FAILED] Duplicate favorite attempt: userId={}, placeId={}", userId, placeId);
            throw new DuplicateFavoriteException();
        }

        Favorite favorite = Favorite.create(user, place);
        Favorite saved = favoriteRepository.save(favorite);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[FAVORITE ADD COMPLETED] favoriteId: {}, executionTime: {} ms", saved.getId(), elapsedTime);

        return FavoriteResponseDto.from(saved);
    }

    /**
     * 특정 장소를 내 즐겨찾기에서 해제(삭제)
     */
    @Transactional
    public void removeFavorite(UUID userId, UUID placeId) {
        long startTime = System.currentTimeMillis();
        log.info("[FAVORITE REMOVE START] userId: {}, placeId: {}", userId, placeId);

        if (!favoriteRepository.existsByUserIdAndPlaceId(userId, placeId)) {
            log.warn("[FAVORITE REMOVE FAILED] Favorite not found for userId: {}, placeId: {}", userId, placeId);
            throw new FavoriteNotFoundException();
        }

        favoriteRepository.deleteByUserIdAndPlaceId(userId, placeId);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[FAVORITE REMOVE COMPLETED] userId: {}, placeId: {}, executionTime: {} ms", userId, placeId, elapsedTime);
    }

    /**
     * 특정 장소의 즐겨찾기 여부 (TSK-BE-030). 없는 장소도 예외 없이 false.
     */
    @Transactional(readOnly = true)
    public boolean isFavorited(UUID userId, UUID placeId) {
        return favoriteRepository.existsByUserIdAndPlaceId(userId, placeId);
    }

    /**
     * 현재 로그인한 사용자의 모든 즐겨찾기 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FavoriteResponseDto> getMyFavorites(UUID userId) {
        long startTime = System.currentTimeMillis();
        log.info("[FAVORITE GET ALL START] userId: {}", userId);

        List<Favorite> favorites = favoriteRepository.findAllByUserId(userId);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[FAVORITE GET ALL COMPLETED] userId: {}, count: {}, executionTime: {} ms", userId, favorites.size(), elapsedTime);

        return favorites.stream()
                .map(FavoriteResponseDto::from)
                .toList();
    }
}
