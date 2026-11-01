package com.Coming.Backend.auth.social;

import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Apple identityToken 클레임. email은 최초 로그인에만 들어오고 비공개 릴레이 주소일 수 있다.
 */
@RequiredArgsConstructor
public class AppleUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> claims;

    @Override
    public String getProviderId() {
        return (String) claims.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) claims.get("email");
    }
}
