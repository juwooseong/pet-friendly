package com.petspot.application.user;

import com.petspot.api.user.dto.MyPageResponseDto;
import com.petspot.domain.favorite.repository.FavoriteRepository;
import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.pet.repository.PetRepository;
import com.petspot.domain.review.repository.ReviewRepository;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 마이페이지 통합 요약 정보 조회 전담 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageQueryService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 현재 로그인 사용자의 마이페이지 요약 정보 조회
     * (사용자 프로필, 대표 반려동물, 등록 펫 수, 즐겨찾기 수, 작성 리뷰 수 일괄 통합)
     */
    public MyPageResponseDto getMyPageSummary(UUID userId) {
        long startTime = System.currentTimeMillis();
        log.info("[MYPAGE GET START] Requesting my page summary for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[MYPAGE GET FAILED] User not found with id: {}", userId);
                    return new UserNotFoundException("사용자를 찾을 수 없습니다.");
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("[MYPAGE GET FAILED] User is inactive: status={}, userId={}", user.getStatus(), userId);
            throw new UserNotFoundException("사용자를 찾을 수 없습니다.");
        }

        // 1. 대표 반려동물 단건 조회 (Optional)
        Optional<Pet> representativePetOpt = petRepository.findRepresentativePet(userId);

        // 2. 개수 카운트 쿼리 최적화 (N+1 문제 배제)
        long petCount = petRepository.countByOwnerId(userId);
        long favoriteCount = favoriteRepository.countByUserId(userId);
        long reviewCount = reviewRepository.countByUserIdAndDeletedFalse(userId);

        log.debug("[MYPAGE DEBUG] userId: {}, repPetPresent: {}, petCount: {}, favoriteCount: {}, reviewCount: {}",
                userId, representativePetOpt.isPresent(), petCount, favoriteCount, reviewCount);

        MyPageResponseDto result = MyPageResponseDto.of(user, representativePetOpt.orElse(null), petCount, favoriteCount, reviewCount);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[MYPAGE GET COMPLETED] userId: {}, executionTime: {} ms", userId, elapsedTime);

        return result;
    }
}
