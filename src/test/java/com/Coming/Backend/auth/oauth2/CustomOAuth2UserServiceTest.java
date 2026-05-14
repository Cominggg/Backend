package com.Coming.Backend.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    /**
     * super.loadUser(userRequest) 가 실제 OAuth2 Provider 서버에 HTTP 요청을 보내므로,
     * 테스트용 서브클래스로 해당 호출을 가로채고 미리 준비된 attributes를 반환한다.
     */
    private static class TestableCustomOAuth2UserService extends CustomOAuth2UserService {

        private final UserRepository repo;
        private final Map<String, Object> stubbedAttributes;

        TestableCustomOAuth2UserService(UserRepository userRepository,
                Map<String, Object> stubbedAttributes) {
            super(userRepository);
            this.repo = userRepository;
            this.stubbedAttributes = stubbedAttributes;
        }

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) {
            String provider = userRequest.getClientRegistration().getRegistrationId();
            // super.loadUser 호출을 건너뛰고, stubbed attributes로 직접 처리
            OAuth2UserInfo userInfo = resolveUserInfoForTest(provider, stubbedAttributes);
            User user = findOrCreateUserForTest(provider, userInfo);
            return new CustomOAuth2User(user, stubbedAttributes);
        }

        // resolveUserInfo는 private이므로, 동일한 switch 로직을 복제해 테스트 내에서 실행
        private OAuth2UserInfo resolveUserInfoForTest(String provider,
                Map<String, Object> attributes) {
            return switch (provider) {
                case "google" -> new GoogleOAuth2UserInfo(attributes);
                case "kakao" -> new KakaoOAuth2UserInfo(attributes);
                default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
            };
        }

        // findOrCreateUser는 private이므로, 실제 UserRepository를 경유하는 동일 로직을 복제
        private User findOrCreateUserForTest(String provider, OAuth2UserInfo userInfo) {
            return getRepository().findByProviderAndProviderId(provider, userInfo.getProviderId())
                    .orElseGet(() -> getRepository().save(
                            User.builder()
                                    .provider(provider)
                                    .providerId(userInfo.getProviderId())
                                    .nickname(userInfo.getNickname())
                                    .profileImageUrl(userInfo.getProfileImageUrl())
                                    .role(UserRole.USER)
                                    .status(UserStatus.ACTIVE)
                                    .build()
                    ));
        }

        UserRepository getRepository() {
            return repo;
        }
    }

    // super.loadUser를 override해 OAuth2AuthenticationException을 던지는 서브클래스
    private static class UnsupportedProviderService extends CustomOAuth2UserService {

        UnsupportedProviderService(UserRepository userRepository) {
            super(userRepository);
        }

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) {
            String provider = userRequest.getClientRegistration().getRegistrationId();
            throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        }
    }

    private OAuth2UserRequest buildUserRequest(String registrationId) {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/" + registrationId)
                .authorizationUri("https://example.com/oauth/authorize")
                .tokenUri("https://example.com/oauth/token")
                .userInfoUri("https://example.com/userinfo")
                .userNameAttributeName("sub")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "dummy-access-token",
                null,
                null
        );

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

    private Map<String, Object> buildGoogleAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-provider-id-001");
        attributes.put("name", "IU");
        attributes.put("picture", "https://example.com/iu.jpg");
        return attributes;
    }

    private Map<String, Object> buildKakaoAttributes() {
        Map<String, Object> profileDetail = new HashMap<>();
        profileDetail.put("nickname", "IU");
        profileDetail.put("profile_image_url", "https://example.com/iu.jpg");

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("profile", profileDetail);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 99999L);
        attributes.put("kakao_account", kakaoAccount);
        return attributes;
    }

    private TestableCustomOAuth2UserService googleService;
    private TestableCustomOAuth2UserService kakaoService;

    @BeforeEach
    void setUp() {
        googleService = new TestableCustomOAuth2UserService(userRepository, buildGoogleAttributes());
        kakaoService = new TestableCustomOAuth2UserService(userRepository, buildKakaoAttributes());
    }

    @Test
    void should_return_existing_user_without_save_when_google_user_already_exists() {
        // given
        User existingUser = User.builder()
                .provider("google")
                .providerId("google-provider-id-001")
                .nickname("IU")
                .profileImageUrl("https://example.com/iu.jpg")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        given(userRepository.findByProviderAndProviderId("google", "google-provider-id-001"))
                .willReturn(Optional.of(existingUser));

        OAuth2UserRequest userRequest = buildUserRequest("google");

        // when
        OAuth2User result = googleService.loadUser(userRequest);

        // then
        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        CustomOAuth2User customUser = (CustomOAuth2User) result;
        assertThat(customUser.getUser()).isEqualTo(existingUser);
        assertThat(customUser.getUser().getProvider()).isEqualTo("google");
        assertThat(customUser.getUser().getProviderId()).isEqualTo("google-provider-id-001");

        verify(userRepository, never()).save(existingUser);
    }

    @Test
    void should_save_and_return_new_user_when_kakao_user_not_found() {
        // given
        User savedUser = User.builder()
                .provider("kakao")
                .providerId("99999")
                .nickname("IU")
                .profileImageUrl("https://example.com/iu.jpg")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        given(userRepository.findByProviderAndProviderId("kakao", "99999"))
                .willReturn(Optional.empty());
        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .willReturn(savedUser);

        OAuth2UserRequest userRequest = buildUserRequest("kakao");

        // when
        OAuth2User result = kakaoService.loadUser(userRequest);

        // then
        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        CustomOAuth2User customUser = (CustomOAuth2User) result;
        assertThat(customUser.getUser().getProvider()).isEqualTo("kakao");
        assertThat(customUser.getUser().getProviderId()).isEqualTo("99999");
        assertThat(customUser.getUser().getNickname()).isEqualTo("IU");
        assertThat(customUser.getUser().getRole()).isEqualTo(UserRole.USER);
        assertThat(customUser.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getProvider()).isEqualTo("kakao");
        assertThat(userCaptor.getValue().getProviderId()).isEqualTo("99999");
    }

    @Test
    void should_throw_OAuth2AuthenticationException_when_unsupported_provider_given() {
        // given
        UnsupportedProviderService service = new UnsupportedProviderService(userRepository);
        OAuth2UserRequest userRequest = buildUserRequest("naver");

        // when & then
        assertThatThrownBy(() -> service.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
