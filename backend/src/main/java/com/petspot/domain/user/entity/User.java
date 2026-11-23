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

    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired;

    /**
     * JWT 버전 (TSK-SEC-001). 토큰에 발급 당시 값을 넣고, 인증할 때 현재 값과 다르면 거부한다.
     * 비밀번호가 바뀌면 올려서 이전에 발급한 토큰을 모두 무효화한다.
     */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

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
            this.tokenVersion++;
        }
    }

    /**
     * 임시 비밀번호 발급 (강제 비밀번호 변경 상태로 전환, 이전 토큰 무효화).
     * <p>비밀번호 찾기는 재설정 링크 방식으로 바뀌어(TSK-SEC-003) 지금은 호출하는 곳이 없다.
     * 관리자 강제 초기화 같은 용도를 위해 강제 변경 흐름(PasswordChangeRequiredInterceptor)과 함께 남겨 둔다.
     */
    public void issueTemporaryPassword(String temporaryPasswordHash) {
        this.passwordHash = temporaryPasswordHash;
        this.passwordChangeRequired = true;
        this.tokenVersion++;
    }

    /**
     * 사용자 본인의 비밀번호 변경 완료 처리.
     * 강제 비밀번호 변경 상태를 해제하고, 이전에 발급한 토큰을 무효화한다.
     */
    public void completePasswordChange(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangeRequired = false;
        this.tokenVersion++;
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
