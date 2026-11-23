package com.petspot.global.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT (JSON Web Token) 생성 및 검증 컴포넌트
 */
@Component
public class JwtTokenProvider {

    private static final String TOKEN_VERSION_CLAIM = "ver";

    private final SecretKey key;

    @Getter
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String email) {
        return generateToken(userId, email, "USER");
    }

    public String generateToken(String userId, String email, String role) {
        return generateToken(userId, email, role, 0);
    }

    /**
     * @param tokenVersion 발급 시점의 사용자 토큰 버전({@code User.tokenVersion}, TSK-SEC-001)
     */
    public String generateToken(String userId, String email, String role, int tokenVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .claim(TOKEN_VERSION_CLAIM, tokenVersion)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return getUserIdFromToken(token);
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public String extractEmail(String token) {
        return getEmailFromToken(token);
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractRole(String token) {
        return getRoleFromToken(token);
    }

    /**
     * 토큰에 담긴 사용자 토큰 버전. 버전이 없는 토큰(도입 전 발급)은 0으로 본다 - 모든 사용자의 시작 버전이 0이라
     * 배포 직후 기존 로그인이 풀리지 않고, 비밀번호를 바꾼 사용자의 예전 토큰만 거부된다.
     */
    public int extractTokenVersion(String token) {
        Integer version = parseClaims(token).get(TOKEN_VERSION_CLAIM, Integer.class);
        return version != null ? version : 0;
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
