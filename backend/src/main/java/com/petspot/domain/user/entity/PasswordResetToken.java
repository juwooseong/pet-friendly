package com.petspot.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 비밀번호 재설정 링크 토큰 (TSK-SEC-003).
 * <p>원문 토큰은 메일 링크에만 담기고 DB에는 SHA-256 해시만 저장한다. 1회용이며 만료 시각이 지나면 쓸 수 없다.
 * 같은 계정에 새 링크를 발급하면 이전에 발급한 미사용 링크는 폐기한다.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private ZonedDateTime expiresAt;

    /** 사용했거나 새 링크 발급으로 폐기된 시각. null이면 아직 유효할 수 있다. */
    @Column(name = "used_at")
    private ZonedDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    private PasswordResetToken(UUID userId, String tokenHash, ZonedDateTime now, ZonedDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public static PasswordResetToken issue(UUID userId, String tokenHash, ZonedDateTime now, ZonedDateTime expiresAt) {
        return new PasswordResetToken(userId, tokenHash, now, expiresAt);
    }

    public boolean isUsable(ZonedDateTime now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    /** 사용 완료 또는 폐기 처리. 이후에는 다시 쓸 수 없다. */
    public void markUsed(ZonedDateTime now) {
        if (usedAt == null) {
            usedAt = now;
        }
    }
}
