package com.petspot.domain.user.repository;

import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 Spring Data JPA Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * 이메일 기반 회원 존재 여부 확인 (중복 가입 방지용)
     */
    boolean existsByEmail(String email);

    /**
     * 이메일 기반 전체 회원 조회 (상태 구분 없음)
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 및 회원 상태(status) 조건 기반 회원 조회
     */
    Optional<User> findByEmailAndStatus(String email, UserStatus status);

    /**
     * 이메일 기반 활성 회원(ACTIVE)만 조회 (로그인 및 인증 처리용)
     */
    default Optional<User> findActiveByEmail(String email) {
        return findByEmailAndStatus(email, UserStatus.ACTIVE);
    }
}
