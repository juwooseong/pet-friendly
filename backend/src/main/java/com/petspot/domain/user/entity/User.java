package com.petspot.domain.user.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 사용자 JPA 엔티티
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "withdraw_at")
    private ZonedDateTime withdrawAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Builder(access = AccessLevel.PROTECTED)
    protected User(UUID id, String email, String passwordHash, String nickname, String avatarUrl,
                   UserRole role, UserStatus status, OAuthProvider provider, ZonedDateTime withdrawAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.role = role != null ? role : UserRole.USER;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.provider = provider != null ? provider : OAuthProvider.LOCAL;
        this.withdrawAt = withdrawAt;
    }

    /**
     * 자체 회원가입 전용 정적 팩토리 메서드 (Static Factory Method)
     * 기본값 설정: role = USER, status = ACTIVE, provider = LOCAL
     */
    public static User register(String email, String passwordHash, String nickname) {
        return register(email, passwordHash, nickname, null);
    }

    public static User register(String email, String passwordHash, String nickname, String avatarUrl) {
        return User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .provider(OAuthProvider.LOCAL)
                .build();
    }

    /**
     * 소셜(OAuth) 회원가입 전용 정적 팩토리 메서드
     */
    public static User registerOAuth(String email, String nickname, String avatarUrl, OAuthProvider provider) {
        return User.builder()
                .email(email)
                .passwordHash("OAUTH_NO_PASSWORD_" + UUID.randomUUID())
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .provider(provider != null ? provider : OAuthProvider.LOCAL)
                .build();
    }

    /**
     * 닉네임 및 프로필 이미지 변경 (도메인 메서드)
     */
    public void updateProfile(String nickname, String avatarUrl) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (avatarUrl != null) {
            this.avatarUrl = avatarUrl;
        }
    }

    /**
     * 비밀번호 변경 (도메인 메서드)
     */
    public void changePassword(String newPasswordHash) {
        if (newPasswordHash != null && !newPasswordHash.isBlank()) {
            this.passwordHash = newPasswordHash;
        }
    }

    /**
     * 계정 상태 변경 (도메인 메서드)
     */
    public void changeStatus(UserStatus newStatus) {
        if (newStatus != null) {
            this.status = newStatus;
        }
    }

    /**
     * 회원 탈퇴 처리 (도메인 메서드)
     */
    public void withdraw() {
        if (this.status == UserStatus.WITHDRAWN) {
            throw new IllegalStateException("이미 탈퇴 처리된 회원입니다.");
        }
        this.status = UserStatus.WITHDRAWN;
        this.withdrawAt = ZonedDateTime.now();
    }
}
