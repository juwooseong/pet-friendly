package com.petspot.application.auth;

import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.api.auth.dto.UserRegisterResponseDto;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 및 인증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 비즈니스 로직 수행
     *
     * @param request 회원가입 요청 DTO
     * @return 회원가입 완료 응답 DTO
     */
    @Transactional
    public UserRegisterResponseDto register(UserRegisterRequestDto request) {
        log.info("[AUTH REGISTER] Registration attempt for email: {}", request.getEmail());

        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("[AUTH REGISTER FAILED] Duplicate email: {}", request.getEmail());
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        // 2. 비밀번호 BCrypt 암호화
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 3. User 정적 팩토리 메서드로 엔티티 생성 (기본값: USER, ACTIVE, LOCAL)
        User newUser = User.register(request.getEmail(), passwordHash, request.getNickname());

        // 4. DB 저장
        User savedUser = userRepository.save(newUser);
        log.info("[AUTH REGISTER SUCCESS] User registered successfully: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        // 5. Response DTO 반환
        return UserRegisterResponseDto.from(savedUser);
    }
}
