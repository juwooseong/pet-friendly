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
    private String password;

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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Builder
    public User(UUID id, String email, String password, String nickname, String avatarUrl,
                UserRole role, UserStatus status, OAuthProvider provider) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.role = role != null ? role : UserRole.USER;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.provider = provider != null ? provider : OAuthProvider.LOCAL;
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
            this.password = newPasswordHash;
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
        this.status = UserStatus.WITHDRAWN;
    }
}
