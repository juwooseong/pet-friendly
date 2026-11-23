package com.petspot.domain.user.repository;

import com.petspot.domain.user.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 비밀번호 재설정 토큰 저장소 (TSK-SEC-003)
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** 새 링크를 발급할 때 폐기할 이전 미사용 토큰 */
    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(UUID userId);

    /** 계정별 요청 횟수 제한: 기준 시각 이후 발급 수 */
    long countByUserIdAndCreatedAtAfter(UUID userId, ZonedDateTime since);
}
