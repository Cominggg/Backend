package com.Coming.Backend.auth.social;

import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;

/**
 * 앱이 기기의 소셜 SDK로 받은 토큰을 제공자 기준으로 검증하고 사용자 식별 정보를 꺼낸다.
 */
public interface SocialTokenVerifier {

    String provider();

    /**
     * @throws com.Coming.Backend.auth.exception.SocialTokenInvalidException 토큰이 유효하지 않거나 우리 앱에서 발급된 것이 아닌 경우
     */
    OAuth2UserInfo verify(String token);
}
