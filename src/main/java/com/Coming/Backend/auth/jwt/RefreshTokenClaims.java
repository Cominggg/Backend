package com.Coming.Backend.auth.jwt;

/**
 * 검증된 Refresh Token에서 추출한 사용자 ID와 세션 식별자(jti).
 */
public record RefreshTokenClaims(Long userId, String sessionId) {
}
