package com.petspot.global.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secretKey = "petspot_super_secret_jwt_key_that_is_at_least_32_bytes_long_for_security";
    private final long expirationMs = 3600000; // 1 시간

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secretKey, expirationMs);
    }

    @Test
    @DisplayName("JWT 토큰 생성 및 Claims (userId, email, role) 정상 파싱 검증")
    void generateAndParseToken_Success() {
        // given
        String userId = UUID.randomUUID().toString();
        String email = "testuser@petspot.com";
        String role = "USER";

        // when
        String token = jwtTokenProvider.generateToken(userId, email, role);

        // then
        assertThat(token).isNotNull().isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(email);
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(role);
        assertThat(jwtTokenProvider.getExpirationMs()).isEqualTo(expirationMs);
    }

    @Test
    @DisplayName("유효하지 않거나 변조된 토큰 검증 시 validateToken은 false를 반환")
    void validateToken_InvalidToken_ReturnsFalse() {
        // given
        String invalidToken = "invalid.jwt.token.string";

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }
}
