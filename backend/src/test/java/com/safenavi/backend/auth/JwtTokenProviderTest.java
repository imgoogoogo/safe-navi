package com.safenavi.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.safenavi.backend.auth.security.JwtProperties;
import com.safenavi.backend.auth.security.JwtTokenProvider;
import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";

    private final JwtTokenProvider provider = new JwtTokenProvider(
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)));

    @Test
    void accessTokenRoundTrip() {
        var user = provider.parse(provider.createAccessToken(7L, "ADMIN"), JwtTokenProvider.TYPE_ACCESS);

        assertThat(user.id()).isEqualTo(7L);
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String refresh = provider.createRefreshToken(7L, "USER");

        assertThatThrownBy(() -> provider.parse(refresh, JwtTokenProvider.TYPE_ACCESS))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    void expiredTokenIsReportedAsExpired() {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                new JwtProperties(SECRET, Duration.ofSeconds(-10), Duration.ofDays(14)));
        String token = shortLived.createAccessToken(1L, "USER");

        assertThatThrownBy(() -> provider.parse(token, JwtTokenProvider.TYPE_ACCESS))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtTokenProvider other = new JwtTokenProvider(
                new JwtProperties("another-secret-that-is-at-least-32-bytes!!", Duration.ofMinutes(30), Duration.ofDays(14)));

        assertThatThrownBy(() -> provider.parse(other.createAccessToken(1L, "USER"), JwtTokenProvider.TYPE_ACCESS))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtTokenProvider(new JwtProperties("short", Duration.ofMinutes(1), Duration.ofDays(1))))
                .isInstanceOf(IllegalStateException.class);
    }
}
