package com.petspot.application.auth;

import com.petspot.api.auth.dto.UserLoginRequestDto;
import com.petspot.api.auth.dto.UserLoginResponseDto;
import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.api.auth.dto.UserRegisterResponseDto;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.DuplicateEmailException;
import com.petspot.global.error.exception.InvalidCredentialsException;
import com.petspot.global.util.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;

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

    /**
     * 로그인 인증 처리 및 JWT 토큰 발급
     *
     * @param request 로그인 요청 DTO (email, password)
     * @return JWT Access Token 응답 DTO
     */
    @Transactional(readOnly = true)
    public UserLoginResponseDto login(UserLoginRequestDto request) {
        log.info("[AUTH LOGIN] Login attempt for email: {}", request.getEmail());

        // 1. 이메일 기반 회원 존재 여부 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("[AUTH LOGIN FAILED] Email not found: {}", request.getEmail());
                    return new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
                });

        // 2. 계정 활성화 상태 확인 (ACTIVE 만 로그인 가능)
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("[AUTH LOGIN FAILED] Inactive or withdrawn status: status={}, email={}", user.getStatus(), request.getEmail());
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 3. 비밀번호 BCrypt 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("[AUTH LOGIN FAILED] Password mismatch for email: {}", request.getEmail());
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 4. JWT Access Token 발급
        String accessToken = jwtTokenProvider.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );

        log.info("[AUTH LOGIN SUCCESS] JWT Token generated for userId: {}", user.getId());

        // 5. Response DTO 반환
        return UserLoginResponseDto.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .build();
    }
}
