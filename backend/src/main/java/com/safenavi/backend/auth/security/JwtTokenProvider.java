package com.safenavi.backend.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";

    private final SecretKey key;
    private final Duration accessExpiration;
    private final Duration refreshExpiration;

    public JwtTokenProvider(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 bytes long (HS256)");
        }
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = properties.accessExpiration();
        this.refreshExpiration = properties.refreshExpiration();
    }

    public String createAccessToken(Long userId, String role) {
        return createToken(userId, role, TYPE_ACCESS, accessExpiration);
    }

    public String createRefreshToken(Long userId, String role) {
        return createToken(userId, role, TYPE_REFRESH, refreshExpiration);
    }

    public Duration getAccessExpiration() {
        return accessExpiration;
    }

    /**
     * Verifies signature, expiry and token type.
     *
     * @throws BusinessException TOKEN_EXPIRED or INVALID_TOKEN
     */
    public AuthUser parse(String token, String expectedType) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        try {
            return new AuthUser(Long.valueOf(claims.getSubject()), claims.get(CLAIM_ROLE, String.class));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String createToken(Long userId, String role, String type, Duration ttl) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key)
                .compact();
    }
}
