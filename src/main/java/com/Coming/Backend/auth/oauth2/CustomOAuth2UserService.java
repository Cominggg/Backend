package com.Coming.Backend.auth.oauth2;

import com.Coming.Backend.auth.exception.UserSuspendedException;
import com.Coming.Backend.auth.service.SocialUserService;
import com.Coming.Backend.auth.service.SocialUserService.SocialUser;
import com.Coming.Backend.common.exception.ErrorCode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final SocialUserService socialUserService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        return toCustomOAuth2User(provider, oAuth2User.getAttributes());
    }

    /**
     * 제공자 응답 attributes로 회원을 조회·생성한다. 정지 계정은 OAuth2FailureHandler가 처리하도록
     * OAuth2AuthenticationException(USER_SUSPENDED)으로 변환한다.
     */
    CustomOAuth2User toCustomOAuth2User(String provider, Map<String, Object> attributes) {
        OAuth2UserInfo userInfo = resolveUserInfo(provider, attributes);
        try {
            SocialUser result = socialUserService.findOrCreate(provider, userInfo);
            return new CustomOAuth2User(result.user(), attributes, result.isNewUser());
        } catch (UserSuspendedException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ErrorCode.USER_SUSPENDED.name()), ErrorCode.USER_SUSPENDED.getMessage());
        }
    }

    private OAuth2UserInfo resolveUserInfo(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        };
    }
}
