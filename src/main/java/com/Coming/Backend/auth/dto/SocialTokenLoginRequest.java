package com.Coming.Backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 앱 소셜 토큰 로그인 요청. token은 Google ID Token·Kakao Access Token·Apple identityToken이고,
 * authorizationCode는 apple일 때만 필수다.
 */
public record SocialTokenLoginRequest(
        @NotBlank
        String token,

        String authorizationCode
) {
}
