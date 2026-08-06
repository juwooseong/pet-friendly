package com.petspot.domain.user.repository;

import com.petspot.domain.user.entity.User;
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
     * 이메일 기반 회원 조회
     */
    Optional<User> findByEmail(String email);
}
