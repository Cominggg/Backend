package com.Coming.Backend.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Coming.Backend.auth.exception.ExpiredTokenException;
import com.Coming.Backend.auth.exception.InvalidTokenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    // Base64-encoded 32-byte secret for HS256
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmcx";
    private static final long ACCESS_EXPIRY = 1_800_000L;  // 30분
    private static final long REFRESH_EXPIRY = 604_800_000L; // 7일

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
    }

    @Test
    void should_returnUserId_when_parseAccessToken() {
        // given
        Long userId = 1L;
        String role = "USER";

        // when
        String token = jwtProvider.generateAccessToken(userId, role);

        // then
        assertThat(jwtProvider.parseClaims(token).getSubject()).isEqualTo(userId.toString());
    }

    @Test
    void should_returnRole_when_parseAccessToken() {
        // given
        String role = "ADMIN";
        String token = jwtProvider.generateAccessToken(1L, role);

        // when
        String parsedRole = jwtProvider.getRole(token);

        // then
        assertThat(parsedRole).isEqualTo(role);
    }

    @Test
    void should_returnUserId_when_parseRefreshToken() {
        // given
        Long userId = 42L;

        // when
        String token = jwtProvider.generateRefreshToken(userId);

        // then
        assertThat(jwtProvider.parseRefreshToken(token).userId()).isEqualTo(userId);
    }

    @Test
    void should_issueDistinctSessionIds_when_generateRefreshTokenTwiceInSameSecond() {
        // given
        Long userId = 1L;

        // when
        String first = jwtProvider.generateRefreshToken(userId);
        String second = jwtProvider.generateRefreshToken(userId);

        // then
        assertThat(first).isNotEqualTo(second);
        assertThat(jwtProvider.parseRefreshToken(first).sessionId())
                .isNotBlank()
                .isNotEqualTo(jwtProvider.parseRefreshToken(second).sessionId());
    }

    @Test
    void should_keepSessionId_when_generateRefreshTokenWithSessionId() {
        // given
        String sessionId = "session-1";

        // when
        String token = jwtProvider.generateRefreshToken(1L, sessionId);

        // then
        assertThat(jwtProvider.parseRefreshToken(token))
                .isEqualTo(new RefreshTokenClaims(1L, sessionId));
    }

    @Test
    void should_throwInvalidTokenException_when_parseRefreshTokenWithAccessToken() {
        // given
        String accessToken = jwtProvider.generateAccessToken(1L, "USER");

        // when & then
        assertThatThrownBy(() -> jwtProvider.parseRefreshToken(accessToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void should_throwInvalidTokenException_when_refreshTokenHasNoJtiOrType() {
        // given — 세션 식별자·토큰 종류 도입 이전 형식의 Refresh Token
        String legacyToken = Jwts.builder()
                .subject("1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRY))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();

        // when & then
        assertThatThrownBy(() -> jwtProvider.parseRefreshToken(legacyToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void should_returnTrue_when_checkAccessTokenType() {
        // given
        String accessToken = jwtProvider.generateAccessToken(1L, "USER");

        // when
        boolean isAccessToken = jwtProvider.isAccessToken(jwtProvider.parseClaims(accessToken));

        // then
        assertThat(isAccessToken).isTrue();
    }

    @Test
    void should_returnFalse_when_checkRefreshTokenAsAccessToken() {
        // given
        String refreshToken = jwtProvider.generateRefreshToken(1L);

        // when
        boolean isAccessToken = jwtProvider.isAccessToken(jwtProvider.parseClaims(refreshToken));

        // then
        assertThat(isAccessToken).isFalse();
    }

    @Test
    void should_returnNull_when_parseRoleFromRefreshToken() {
        // given
        String token = jwtProvider.generateRefreshToken(1L);

        // when
        String role = jwtProvider.getRole(token);

        // then
        assertThat(role).isNull();
    }

    @Test
    void should_returnPositiveRemainingExpiry_when_tokenIsValid() {
        // given
        String token = jwtProvider.generateAccessToken(1L, "USER");

        // when
        long remaining = jwtProvider.getRemainingExpiry(token);

        // then
        assertThat(remaining).isPositive().isLessThanOrEqualTo(ACCESS_EXPIRY);
    }

    @Test
    void should_throwInvalidTokenException_when_tokenIsTampered() {
        // given
        String token = jwtProvider.generateAccessToken(1L, "USER") + "tampered";

        // when & then
        assertThatThrownBy(() -> jwtProvider.validateToken(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void should_throwExpiredTokenException_when_tokenIsExpired() {
        // given — 만료시간 0ms로 즉시 만료 토큰 생성
        JwtProvider shortLivedProvider = new JwtProvider(SECRET, 0L, REFRESH_EXPIRY);
        String token = shortLivedProvider.generateAccessToken(1L, "USER");

        // when & then
        assertThatThrownBy(() -> jwtProvider.validateToken(token))
                .isInstanceOf(ExpiredTokenException.class);
    }

    @Test
    void should_throwInvalidTokenException_when_tokenIsBlank() {
        // when & then
        assertThatThrownBy(() -> jwtProvider.validateToken(""))
                .isInstanceOf(InvalidTokenException.class);
    }
}
