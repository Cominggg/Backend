package com.Coming.Backend.auth.oauth2;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    private record UserResult(User user, boolean isNewUser) {}

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo userInfo = resolveUserInfo(provider, attributes);
        UserResult result = findOrCreateUser(provider, userInfo);
        return new CustomOAuth2User(result.user(), attributes, result.isNewUser());
    }

    private OAuth2UserInfo resolveUserInfo(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        };
    }

    private UserResult findOrCreateUser(String provider, OAuth2UserInfo userInfo) {
        Optional<User> existing = userRepository.findByProviderAndProviderId(provider, userInfo.getProviderId());
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getStatus() == UserStatus.SUSPENDED) {
                log.warn("정지된 계정 로그인 시도 — userId: {}", user.getId());
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(ErrorCode.USER_SUSPENDED.name()), "정지된 계정입니다.");
            }
            if (user.getStatus() == UserStatus.INACTIVE) {
                log.info("탈퇴 후 재가입 처리 — userId: {}", user.getId());
                user.reactivate();
                user.updateEmail(userInfo.getEmail());
                return new UserResult(user, true);
            }
            user.updateEmail(userInfo.getEmail());
            return new UserResult(user, false);
        }
        log.info("신규 OAuth2 사용자 생성 — provider: {}, providerId: {}", provider, userInfo.getProviderId());
        try {
            return new UserResult(userRepository.save(
                    User.builder()
                            .email(userInfo.getEmail())
                            .provider(provider)
                            .providerId(userInfo.getProviderId())
                            .role(UserRole.PENDING)
                            .status(UserStatus.ACTIVE)
                            .build()
            ), true);
        } catch (DataIntegrityViolationException e) {
            log.info("동시 최초 로그인 경합 — 기존 사용자 재조회. provider: {}, providerId: {}", provider, userInfo.getProviderId());
            User user = userRepository.findByProviderAndProviderId(provider, userInfo.getProviderId())
                    .orElseThrow(() -> e);
            return new UserResult(user, false);
        }
    }
}
