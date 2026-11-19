package com.petspot.application.user;

import com.petspot.api.user.dto.UserProfileResponseDto;
import com.petspot.api.user.dto.UserProfileUpdateRequestDto;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.DuplicateNicknameException;
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
                    return new UserNotFoundException();
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("[PROFILE GET FAILED] User status is not active: status={}, id={}", user.getStatus(), userId);
            throw new UserNotFoundException();
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
                    return new UserNotFoundException();
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("[PROFILE UPDATE FAILED] User status is not active: status={}, id={}", user.getStatus(), userId);
            throw new UserNotFoundException();
        }

        // TSK-BE-033: 닉네임을 바꾸는 경우에만 중복을 검사한다(본인의 현재 닉네임 유지는 허용).
        // 회원가입과 같은 규칙 - 아이디 찾기가 닉네임으로 회원을 조회하므로, 중복을 허용하면
        // 다른 회원의 (마스킹된) 이메일이 반환될 수 있다.
        if (!request.getNickname().equals(user.getNickname())
                && userRepository.existsByNickname(request.getNickname())) {
            log.warn("[PROFILE UPDATE FAILED] Duplicate nickname requested by userId: {}", userId);
            throw new DuplicateNicknameException();
        }

        // 엔티티 캡슐화된 도메인 메서드 사용 (Setter 배제)
        user.updateProfile(request.getNickname(), request.getAvatarUrl());

        log.info("[PROFILE UPDATE SUCCESS] Profile updated for userId: {}", userId);
        return UserProfileResponseDto.from(user);
    }
}
