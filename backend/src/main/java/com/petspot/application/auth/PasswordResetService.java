package com.petspot.application.auth;

import com.petspot.api.auth.dto.ResetPasswordRequestDto;
import com.petspot.domain.user.entity.PasswordResetToken;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.PasswordResetTokenRepository;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.InvalidPasswordResetTokenException;
import com.petspot.infrastructure.email.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 비밀번호 찾기(재설정 링크) 서비스 (TSK-SEC-003).
 * <p>요청만으로는 비밀번호·로그인 상태가 바뀌지 않는다. 메일로 받은 1회용 링크(30분)에서 새 비밀번호를 정할 때만
 * 바뀌며, 이때 토큰 버전을 올려 기존 로그인도 모두 끊는다(TSK-SEC-001).
 * <p>계정 존재 여부는 응답으로 드러나지 않는다: 없는 이메일·요청 횟수 초과도 같은 성공 응답이고, 메일은 비동기로 보낸다.
 */
@Slf4j
@Service
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
    static final int RATE_LIMIT_MAX_REQUESTS = 3;
    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final TransactionTemplate transactionTemplate;
    private final String frontendBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                EmailSender emailSender,
                                PlatformTransactionManager transactionManager,
                                @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    /**
     * 재설정 링크를 발급해 메일로 보낸다. 가입되지 않은 이메일이나 횟수 초과여도 조용히 끝난다.
     * 토큰 저장(트랜잭션)이 끝난 뒤에 메일을 보내, 메일이 실패해도 DB 상태와 무관하고 저장 실패 시 링크가 나가지 않는다.
     */
    public void requestReset(String email) {
        Optional<ResetMail> mail = transactionTemplate.execute(status -> issueToken(email));
        if (mail != null) {
            mail.ifPresent(m -> emailSender.sendPasswordResetLink(m.email(), m.link()));
        }
    }

    private Optional<ResetMail> issueToken(String email) {
        Optional<User> found = userRepository.findActiveByEmail(email);
        if (found.isEmpty()) {
            log.info("[AUTH PASSWORD RESET] No active account matched for given email");
            return Optional.empty();
        }
        User user = found.get();
        ZonedDateTime now = ZonedDateTime.now();

        long recent = tokenRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minus(RATE_LIMIT_WINDOW));
        if (recent >= RATE_LIMIT_MAX_REQUESTS) {
            log.warn("[AUTH PASSWORD RESET] Rate limited: userId={}, recentRequests={}", user.getId(), recent);
            return Optional.empty();
        }

        // 새 링크만 유효하도록 이전 미사용 링크는 폐기
        tokenRepository.findByUserIdAndUsedAtIsNull(user.getId()).forEach(t -> t.markUsed(now));

        String rawToken = newRawToken();
        tokenRepository.save(PasswordResetToken.issue(user.getId(), sha256(rawToken), now, now.plus(TOKEN_TTL)));
        log.info("[AUTH PASSWORD RESET] Reset link issued for userId: {}", user.getId());

        return Optional.of(new ResetMail(user.getEmail(), frontendBaseUrl + "/reset-password?token=" + rawToken));
    }

    /**
     * 링크의 토큰으로 새 비밀번호를 설정한다. 토큰은 1회용이며, 성공하면 기존 로그인(토큰)이 모두 무효화된다.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        ZonedDateTime now = ZonedDateTime.now();

        PasswordResetToken token = tokenRepository.findByTokenHash(sha256(request.getToken()))
                .filter(t -> t.isUsable(now))
                .orElseThrow(InvalidPasswordResetTokenException::new);
        User user = userRepository.findById(token.getUserId())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        user.completePasswordChange(passwordEncoder.encode(request.getNewPassword()));
        token.markUsed(now);
        log.info("[AUTH PASSWORD RESET SUCCESS] Password reset for userId: {}", user.getId());
    }

    private String newRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private record ResetMail(String email, String link) {
    }
}
