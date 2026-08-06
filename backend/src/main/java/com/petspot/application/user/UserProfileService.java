package com.petspot.application.user;

import com.petspot.api.user.dto.UserProfileResponseDto;
import com.petspot.api.user.dto.UserProfileUpdateRequestDto;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 마이페이지 사용자 프로필 조회 및 수정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    /**
     * 내 정보 조회 (GET /api/v1/users/me)
     */
    @Transactional(readOnly = true)
    public UserProfileResponseDto getMyProfile(UUID userId) {
        log.info("[PROFILE GET] Fetching profile for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[PROFILE GET FAILED] User not found with id: {}", userId);
                    return new UserNotFoundException("사용자를 찾을 수 없습니다.");
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("[PROFILE GET FAILED] User status is not active: status={}, id={}", user.getStatus(), userId);
            throw new UserNotFoundException("사용자를 찾을 수 없습니다.");
        }

        return UserProfileResponseDto.from(user);
    }

    /**
     * 내 프로필 수정 (PUT /api/v1/users/me)
     */
    @Transactional
    public UserProfileResponseDto updateProfile(UUID userId, UserProfileUpdateRequestDto request) {
        log.info("[PROFILE UPDATE] Updating profile for userId: {}, newNickname: {}", userId, request.getNickname());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[PROFILE UPDATE FAILED] User not found with id: {}", userId);
                    return new UserNotFoundException("사용자를 찾을 수 없습니다.");
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("[PROFILE UPDATE FAILED] User status is not active: status={}, id={}", user.getStatus(), userId);
            throw new UserNotFoundException("사용자를 찾을 수 없습니다.");
        }

        // 엔티티 캡슐화된 도메인 메서드 사용 (Setter 배제)
        user.updateProfile(request.getNickname(), request.getAvatarUrl());

        log.info("[PROFILE UPDATE SUCCESS] Profile updated for userId: {}", userId);
        return UserProfileResponseDto.from(user);
    }
}
