package com.Coming.Backend.auth.dto;

/**
 * 앱 로그아웃 요청. refreshToken이 없으면 세션 삭제는 건너뛴다(세션은 TTL 만료로 정리).
 */
public record AppLogoutRequest(String refreshToken) {
}
