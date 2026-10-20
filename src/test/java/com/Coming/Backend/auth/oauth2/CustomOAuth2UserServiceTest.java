package com.Coming.Backend.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.exception.UserSuspendedException;
import com.Coming.Backend.auth.service.SocialUserService;
import com.Coming.Backend.auth.service.SocialUserService.SocialUser;
import com.Coming.Backend.common.exception.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * loadUser는 super.loadUser가 제공자 서버에 HTTP 요청을 보내므로, 그 이후 로직인 toCustomOAuth2User를 직접 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Mock
    private SocialUserService socialUserService;

    private static final Map<String, Object> GOOGLE_ATTRIBUTES = Map.of(
            "sub", "google-provider-id-001",
            "email", "iu@gmail.com");

    private static final Map<String, Object> KAKAO_ATTRIBUTES = Map.of(
            "id", 99999L,
            "kakao_account", Map.of("email", "iu@kakao.com"));

    private User buildUser(String provider, String providerId, UserRole role) {
        return User.builder()
                .provider(provider)
                .providerId(providerId)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void should_return_custom_oauth2_user_with_result_of_social_user_service_when_existing_user_logs_in() {
        // given
        User user = buildUser("google", "google-provider-id-001", UserRole.USER);
        given(socialUserService.findOrCreate(eq("google"), any(OAuth2UserInfo.class)))
                .willReturn(new SocialUser(user, false));

        // when
        CustomOAuth2User result = customOAuth2UserService.toCustomOAuth2User("google", GOOGLE_ATTRIBUTES);

        // then
        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.isNewUser()).isFalse();
        assertThat(result.getAttributes()).isEqualTo(GOOGLE_ATTRIBUTES);
    }

    @Test
    void should_return_isNewUser_true_when_social_user_service_returns_new_user() {
        // given
        User user = buildUser("kakao", "99999", UserRole.PENDING);
        given(socialUserService.findOrCreate(eq("kakao"), any(OAuth2UserInfo.class)))
                .willReturn(new SocialUser(user, true));

        // when
        CustomOAuth2User result = customOAuth2UserService.toCustomOAuth2User("kakao", KAKAO_ATTRIBUTES);

        // then
        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.isNewUser()).isTrue();
    }

    @Test
    void should_pass_google_provider_id_and_email_to_social_user_service_when_google_attributes_given() {
        // given
        User user = buildUser("google", "google-provider-id-001", UserRole.USER);
        given(socialUserService.findOrCreate(eq("google"), any(OAuth2UserInfo.class)))
                .willReturn(new SocialUser(user, false));

        // when
        customOAuth2UserService.toCustomOAuth2User("google", GOOGLE_ATTRIBUTES);

        // then
        ArgumentCaptor<OAuth2UserInfo> captor = ArgumentCaptor.forClass(OAuth2UserInfo.class);
        verify(socialUserService).findOrCreate(eq("google"), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(GoogleOAuth2UserInfo.class);
        assertThat(captor.getValue().getProviderId()).isEqualTo("google-provider-id-001");
        assertThat(captor.getValue().getEmail()).isEqualTo("iu@gmail.com");
    }

    @Test
    void should_pass_kakao_provider_id_and_email_to_social_user_service_when_kakao_attributes_given() {
        // given
        User user = buildUser("kakao", "99999", UserRole.PENDING);
        given(socialUserService.findOrCreate(eq("kakao"), any(OAuth2UserInfo.class)))
                .willReturn(new SocialUser(user, true));

        // when
        customOAuth2UserService.toCustomOAuth2User("kakao", KAKAO_ATTRIBUTES);

        // then
        ArgumentCaptor<OAuth2UserInfo> captor = ArgumentCaptor.forClass(OAuth2UserInfo.class);
        verify(socialUserService).findOrCreate(eq("kakao"), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(KakaoOAuth2UserInfo.class);
        assertThat(captor.getValue().getProviderId()).isEqualTo("99999");
        assertThat(captor.getValue().getEmail()).isEqualTo("iu@kakao.com");
    }

    @Test
    void should_throw_OAuth2AuthenticationException_with_USER_SUSPENDED_when_suspended_user_logs_in() {
        // given
        given(socialUserService.findOrCreate(eq("google"), any(OAuth2UserInfo.class)))
                .willThrow(new UserSuspendedException());

        // when & then
        assertThatThrownBy(() -> customOAuth2UserService.toCustomOAuth2User("google", GOOGLE_ATTRIBUTES))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(ErrorCode.USER_SUSPENDED.name());
    }

    @Test
    void should_throw_OAuth2AuthenticationException_without_calling_social_user_service_when_unsupported_provider_given() {
        // given
        Map<String, Object> naverAttributes = Map.of("id", "naver-1");

        // when & then
        assertThatThrownBy(() -> customOAuth2UserService.toCustomOAuth2User("naver", naverAttributes))
                .isInstanceOf(OAuth2AuthenticationException.class);
        verifyNoInteractions(socialUserService);
    }
}
