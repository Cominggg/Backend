package com.Coming.Backend.auth.jwt;

import com.Coming.Backend.auth.exception.ExpiredTokenException;
import com.Coming.Backend.auth.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey secretKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 새 세션의 Refresh Token을 발급한다. 세션 식별자(jti)는 새로 생성된다.
     */
    public String generateRefreshToken(Long userId) {
        return generateRefreshToken(userId, UUID.randomUUID().toString());
    }

    /**
     * 기존 세션의 Refresh Token을 재발급한다. 회전 시 세션 식별자(jti)를 유지하기 위해 사용한다.
     */
    public String generateRefreshToken(Long userId, String sessionId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .id(sessionId)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpiry))
                .signWith(secretKey)
                .compact();
    }

    public void validateToken(String token) {
        parseClaims(token);
    }

    /**
     * Refresh Token을 검증하고 사용자 ID와 세션 식별자를 반환한다.
     * Access Token이나 세션 식별자(jti) 도입 이전에 발급된 토큰이면 {@link InvalidTokenException}.
     */
    public RefreshTokenClaims parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!REFRESH_TOKEN_TYPE.equals(tokenType) || claims.getId() == null) {
            throw new InvalidTokenException();
        }
        return new RefreshTokenClaims(Long.parseLong(claims.getSubject()), claims.getId());
    }

    /**
     * API 인증에 사용할 수 있는 Access Token인지 확인한다. Refresh Token은 서명이 유효해도 false.
     */
    public boolean isAccessToken(Claims claims) {
        return ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public long getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }

    public long getRemainingExpiry(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException();
        }
    }
}
