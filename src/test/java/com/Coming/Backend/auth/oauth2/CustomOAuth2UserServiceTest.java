package com.Coming.Backend.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.common.exception.ErrorCode;
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
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    /**
     * super.loadUser(userRequest)가 실제 OAuth2 Provider 서버에 HTTP 요청을 보내므로,
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

        private record UserHolder(User user, boolean isNewUser) {}

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) {
            String provider = userRequest.getClientRegistration().getRegistrationId();
            OAuth2UserInfo userInfo = resolveUserInfoForTest(provider, stubbedAttributes);
            UserHolder holder = findOrCreateUserForTest(provider, userInfo);
            return new CustomOAuth2User(holder.user(), stubbedAttributes, holder.isNewUser());
        }

        private OAuth2UserInfo resolveUserInfoForTest(String provider,
                Map<String, Object> attributes) {
            return switch (provider) {
                case "google" -> new GoogleOAuth2UserInfo(attributes);
                case "kakao" -> new KakaoOAuth2UserInfo(attributes);
                default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
            };
        }

        private UserHolder findOrCreateUserForTest(String provider, OAuth2UserInfo userInfo) {
            Optional<User> existing = repo.findByProviderAndProviderId(provider, userInfo.getProviderId());
            if (existing.isPresent()) {
                User user = existing.get();
                if (user.getStatus() == UserStatus.SUSPENDED) {
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error(ErrorCode.USER_SUSPENDED.name()), "정지된 계정입니다.");
                }
                if (user.getStatus() == UserStatus.INACTIVE) {
                    user.reactivate();
                    return new UserHolder(user, true);
                }
                return new UserHolder(user, false);
            }
            return new UserHolder(repo.save(
                    User.builder()
                            .provider(provider)
                            .providerId(userInfo.getProviderId())
                            .role(UserRole.PENDING)
                            .status(UserStatus.ACTIVE)
                            .build()
            ), true);
        }
    }

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
        return attributes;
    }

    private Map<String, Object> buildKakaoAttributes() {
        Map<String, Object> profileDetail = new HashMap<>();
        profileDetail.put("nickname", "IU");

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
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        given(userRepository.findByProviderAndProviderId("google", "google-provider-id-001"))
                .willReturn(Optional.of(existingUser));

        // when
        OAuth2User result = googleService.loadUser(buildUserRequest("google"));

        // then
        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        CustomOAuth2User customUser = (CustomOAuth2User) result;
        assertThat(customUser.getUser()).isEqualTo(existingUser);
        assertThat(customUser.isNewUser()).isFalse();
        verify(userRepository, never()).save(existingUser);
    }

    @Test
    void should_save_pending_user_and_return_isNewUser_true_when_kakao_user_not_found() {
        // given
        User savedUser = User.builder()
                .provider("kakao")
                .providerId("99999")
                .role(UserRole.PENDING)
                .status(UserStatus.ACTIVE)
                .build();

        given(userRepository.findByProviderAndProviderId("kakao", "99999"))
                .willReturn(Optional.empty());
        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .willReturn(savedUser);

        // when
        OAuth2User result = kakaoService.loadUser(buildUserRequest("kakao"));

        // then
        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        CustomOAuth2User customUser = (CustomOAuth2User) result;
        assertThat(customUser.getUser().getRole()).isEqualTo(UserRole.PENDING);
        assertThat(customUser.isNewUser()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getProvider()).isEqualTo("kakao");
        assertThat(userCaptor.getValue().getProviderId()).isEqualTo("99999");
        assertThat(userCaptor.getValue().getNickname()).isNull();
    }

    @Test
    void should_reactivate_and_return_isNewUser_true_when_inactive_user_logs_in() {
        // given
        User inactiveUser = User.builder()
                .provider("google")
                .providerId("google-provider-id-001")
                .role(UserRole.USER)
                .status(UserStatus.INACTIVE)
                .nickname("IU")
                .birthYear(1993)
                .build();

        given(userRepository.findByProviderAndProviderId("google", "google-provider-id-001"))
                .willReturn(Optional.of(inactiveUser));

        // when
        OAuth2User result = googleService.loadUser(buildUserRequest("google"));

        // then
        CustomOAuth2User customUser = (CustomOAuth2User) result;
        assertThat(customUser.isNewUser()).isTrue();
        assertThat(customUser.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(customUser.getUser().getRole()).isEqualTo(UserRole.PENDING);
        assertThat(customUser.getUser().getNickname()).isNull();
        assertThat(customUser.getUser().getBirthYear()).isNull();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void should_throw_OAuth2AuthenticationException_when_suspended_user_logs_in() {
        // given
        User suspendedUser = User.builder()
                .provider("google")
                .providerId("google-provider-id-001")
                .role(UserRole.USER)
                .status(UserStatus.SUSPENDED)
                .build();

        given(userRepository.findByProviderAndProviderId("google", "google-provider-id-001"))
                .willReturn(Optional.of(suspendedUser));

        // when & then
        assertThatThrownBy(() -> googleService.loadUser(buildUserRequest("google")))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(ErrorCode.USER_SUSPENDED.name());
    }

    @Test
    void should_throw_OAuth2AuthenticationException_when_unsupported_provider_given() {
        // given
        UnsupportedProviderService service = new UnsupportedProviderService(userRepository);

        // when & then
        assertThatThrownBy(() -> service.loadUser(buildUserRequest("naver")))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
