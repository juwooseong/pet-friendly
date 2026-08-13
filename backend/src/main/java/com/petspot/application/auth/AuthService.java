package com.petspot.application.auth;

import com.petspot.api.auth.dto.ChangePasswordRequestDto;
import com.petspot.api.auth.dto.FindIdRequestDto;
import com.petspot.api.auth.dto.FindIdResponseDto;
import com.petspot.api.auth.dto.FindPasswordRequestDto;
import com.petspot.api.auth.dto.UserLoginRequestDto;
import com.petspot.api.auth.dto.UserLoginResponseDto;
import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.api.auth.dto.UserRegisterResponseDto;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.DuplicateEmailException;
import com.petspot.global.error.exception.DuplicateNicknameException;
import com.petspot.global.error.exception.InvalidCredentialsException;
import com.petspot.global.error.exception.UserNotFoundException;
import com.petspot.global.util.EmailMasker;
import com.petspot.global.util.JwtTokenProvider;
import com.petspot.global.util.TemporaryPasswordGenerator;
import com.petspot.infrastructure.email.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final EmailSender emailSender;

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

        // 1-1. 닉네임 중복 검사
        if (userRepository.existsByNickname(request.getNickname())) {
            log.warn("[AUTH REGISTER FAILED] Duplicate nickname: {}", request.getNickname());
            throw new DuplicateNicknameException("이미 사용 중인 닉네임입니다.");
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

        // 5. Response DTO 반환 (임시 비밀번호 로그인인 경우 강제 비밀번호 변경 필요 여부 및 사용자 정보 포함)
        return UserLoginResponseDto.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .requiresPasswordChange(user.isPasswordChangeRequired())
                .user(UserLoginResponseDto.UserSummaryDto.from(user))
                .build();
    }

    /**
     * 닉네임으로 가입된 계정의 마스킹된 이메일을 조회한다 (아이디 찾기)
     *
     * @param request 아이디 찾기 요청 DTO (nickname)
     * @return 마스킹된 이메일 응답 DTO
     */
    @Transactional(readOnly = true)
    public FindIdResponseDto findId(FindIdRequestDto request) {
        log.info("[AUTH FIND ID] Find-id attempt for nickname");

        User user = userRepository.findFirstByNicknameAndStatus(request.getNickname(), UserStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn("[AUTH FIND ID FAILED] No matching active user for given nickname");
                    return new UserNotFoundException("일치하는 회원 정보를 찾을 수 없습니다.");
                });

        log.info("[AUTH FIND ID SUCCESS] Matched userId: {}", user.getId());

        return FindIdResponseDto.builder()
                .maskedEmail(EmailMasker.mask(user.getEmail()))
                .build();
    }

    /**
     * 이메일로 임시 비밀번호를 발급하고 메일로 발송한다 (비밀번호 찾기).
     * 계정 존재 여부 노출을 방지하기 위해 이메일이 존재하지 않아도 예외를 발생시키지 않고 조용히 종료한다.
     *
     * @param request 비밀번호 찾기 요청 DTO (email)
     */
    @Transactional
    public void findPassword(FindPasswordRequestDto request) {
        log.info("[AUTH FIND PASSWORD] Find-password attempt for email");

        userRepository.findActiveByEmail(request.getEmail()).ifPresentOrElse(user -> {
            String temporaryPassword = temporaryPasswordGenerator.generate();
            String temporaryPasswordHash = passwordEncoder.encode(temporaryPassword);

            user.issueTemporaryPassword(temporaryPasswordHash);

            emailSender.sendTemporaryPassword(user.getEmail(), temporaryPassword);

            log.info("[AUTH FIND PASSWORD SUCCESS] Temporary password issued for userId: {}", user.getId());
        }, () -> log.info("[AUTH FIND PASSWORD] No active account matched for given email"));
    }

    /**
     * 로그인한 사용자의 비밀번호를 변경한다.
     * 임시 비밀번호로 로그인한 사용자는 이 메서드 호출을 통해 강제 변경 상태를 해제해야 한다.
     *
     * @param userId  현재 로그인한 사용자 ID
     * @param request 비밀번호 변경 요청 DTO (newPassword, confirmPassword)
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequestDto request) {
        log.info("[AUTH CHANGE PASSWORD] Change password attempt for userId: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("[AUTH CHANGE PASSWORD FAILED] New password and confirmation do not match: userId={}", userId);
            throw new IllegalArgumentException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.completePasswordChange(newPasswordHash);

        log.info("[AUTH CHANGE PASSWORD SUCCESS] Password changed for userId: {}", userId);
    }
}
