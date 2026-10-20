package com.Coming.Backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.Coming.Backend.auth.dto.LoginNonceResponse;
import com.Coming.Backend.auth.dto.SocialLoginResponse;
import com.Coming.Backend.auth.dto.SocialTokenLoginRequest;
import com.Coming.Backend.auth.dto.TokenPair;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import com.Coming.Backend.auth.exception.UserSuspendedException;
import com.Coming.Backend.auth.oauth2.GoogleOAuth2UserInfo;
import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;
import com.Coming.Backend.auth.repository.LoginNonceRepository;
import com.Coming.Backend.auth.service.SocialUserService.SocialUser;
import com.Coming.Backend.auth.social.AppleAuthClient;
import com.Coming.Backend.auth.social.AppleAuthClient.AppleTokens;
import com.Coming.Backend.auth.social.AppleIdentityTokenVerifier;
import com.Coming.Backend.auth.social.AppleUserInfo;
import com.Coming.Backend.auth.social.SocialTokenVerifier;
import com.Coming.Backend.common.exception.InvalidInputException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    @Mock
    private SocialTokenVerifier googleVerifier;

    @Mock
    private AppleIdentityTokenVerifier appleVerifier;

    @Mock
    private SocialUserService socialUserService;

    @Mock
    private AuthService authService;

    @Mock
    private AppleAuthClient appleAuthClient;

    @Mock
    private LoginNonceRepository loginNonceRepository;

    private SocialLoginService socialLoginService;

    private static final Long USER_ID = 1L;
    private static final String GOOGLE_ID_TOKEN = "google-id-token";
    private static final String APPLE_IDENTITY_TOKEN = "apple-identity-token";
    private static final String AUTHORIZATION_CODE = "apple-authorization-code";
    private static final String APPLE_REFRESH_TOKEN = "apple-refresh-token";
    private static final String APPLE_CODE_ID_TOKEN = "apple-code-id-token";
    private static final String APPLE_SUB = "001234.abcdef0123456789.0123";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    private final OAuth2UserInfo googleUserInfo =
            new GoogleOAuth2UserInfo(Map.of("sub", "google-provider-id-001", "email", "iu@gmail.com"));
    private final OAuth2UserInfo appleUserInfo = new AppleUserInfo(Map.of("sub", APPLE_SUB));

    @BeforeEach
    void setUp() {
        given(googleVerifier.provider()).willReturn("google");
        given(appleVerifier.provider()).willReturn("apple");
        socialLoginService = new SocialLoginService(List.of(googleVerifier, appleVerifier),
                socialUserService, authService, appleAuthClient, appleVerifier, loginNonceRepository);
    }

    private User buildUser(String provider, UserRole role) {
        return User.builder()
                .id(USER_ID)
                .provider(provider)
                .providerId(provider + "-provider-id")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private void givenTokensIssued(User user) {
        given(authService.issueTokens(user)).willReturn(new TokenPair(ACCESS_TOKEN, REFRESH_TOKEN));
    }

    // -------------------------------------------------------------------------
    // issueNonce
    // -------------------------------------------------------------------------

    @Test
    void should_save_43_char_base64url_nonce_for_5_minutes_when_issue_nonce() {
        // when
        LoginNonceResponse response = socialLoginService.issueNonce();

        // then
        assertThat(response.nonce()).hasSize(43).matches("[A-Za-z0-9_-]+");
        verify(loginNonceRepository).save(response.nonce(), Duration.ofMinutes(5));
    }

    @Test
    void should_issue_different_nonce_each_time() {
        // when
        String first = socialLoginService.issueNonce().nonce();
        String second = socialLoginService.issueNonce().nonce();

        // then
        assertThat(first).isNotEqualTo(second);
    }

    // -------------------------------------------------------------------------
    // google
    // -------------------------------------------------------------------------

    @Test
    void should_issue_tokens_with_isNewUser_true_when_google_login_for_pending_user() {
        // given
        given(googleVerifier.verify(GOOGLE_ID_TOKEN)).willReturn(googleUserInfo);
        User user = buildUser("google", UserRole.PENDING);
        given(socialUserService.findOrCreate("google", googleUserInfo)).willReturn(new SocialUser(user, true));
        givenTokensIssued(user);

        // when
        SocialLoginResponse response =
                socialLoginService.login("google", new SocialTokenLoginRequest(GOOGLE_ID_TOKEN, null));

        // then
        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(response.isNewUser()).isTrue();
        verify(authService).issueTokens(user);
    }

    @Test
    void should_return_isNewUser_false_when_google_login_for_registered_user() {
        // given
        given(googleVerifier.verify(GOOGLE_ID_TOKEN)).willReturn(googleUserInfo);
        User user = buildUser("google", UserRole.USER);
        given(socialUserService.findOrCreate("google", googleUserInfo)).willReturn(new SocialUser(user, false));
        givenTokensIssued(user);

        // when
        SocialLoginResponse response =
                socialLoginService.login("google", new SocialTokenLoginRequest(GOOGLE_ID_TOKEN, null));

        // then
        assertThat(response.isNewUser()).isFalse();
    }

    @Test
    void should_not_interact_with_apple_auth_client_when_google_login() {
        // given
        given(googleVerifier.verify(GOOGLE_ID_TOKEN)).willReturn(googleUserInfo);
        User user = buildUser("google", UserRole.USER);
        given(socialUserService.findOrCreate("google", googleUserInfo)).willReturn(new SocialUser(user, false));
        givenTokensIssued(user);

        // when
        socialLoginService.login("google", new SocialTokenLoginRequest(GOOGLE_ID_TOKEN, AUTHORIZATION_CODE));

        // then
        verifyNoInteractions(appleAuthClient);
        verify(appleVerifier, never()).extractSubject(anyString());
        verify(socialUserService, never()).findOrCreateAppleUser(any(), any());
    }

    // -------------------------------------------------------------------------
    // apple
    // -------------------------------------------------------------------------

    @Test
    void should_find_or_create_apple_user_with_refresh_token_when_code_subject_matches() {
        // given
        given(appleVerifier.verify(APPLE_IDENTITY_TOKEN)).willReturn(appleUserInfo);
        given(appleAuthClient.exchangeAuthorizationCode(AUTHORIZATION_CODE))
                .willReturn(new AppleTokens(APPLE_REFRESH_TOKEN, APPLE_CODE_ID_TOKEN));
        given(appleVerifier.extractSubject(APPLE_CODE_ID_TOKEN)).willReturn(APPLE_SUB);
        User user = buildUser("apple", UserRole.PENDING);
        given(socialUserService.findOrCreateAppleUser(appleUserInfo, APPLE_REFRESH_TOKEN))
                .willReturn(new SocialUser(user, true));
        givenTokensIssued(user);

        // when
        SocialLoginResponse response = socialLoginService.login("apple",
                new SocialTokenLoginRequest(APPLE_IDENTITY_TOKEN, AUTHORIZATION_CODE));

        // then
        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(response.isNewUser()).isTrue();
        verify(socialUserService).findOrCreateAppleUser(appleUserInfo, APPLE_REFRESH_TOKEN);
        verify(socialUserService, never()).findOrCreate(anyString(), any());
    }

    @Test
    void should_throw_SocialTokenInvalidException_and_not_create_session_when_code_subject_differs() {
        // given — 다른 Apple 사용자의 authorizationCode를 끼워 넣은 경우
        given(appleVerifier.verify(APPLE_IDENTITY_TOKEN)).willReturn(appleUserInfo);
        given(appleAuthClient.exchangeAuthorizationCode(AUTHORIZATION_CODE))
                .willReturn(new AppleTokens(APPLE_REFRESH_TOKEN, APPLE_CODE_ID_TOKEN));
        given(appleVerifier.extractSubject(APPLE_CODE_ID_TOKEN)).willReturn("009999.other-apple-user.0001");

        // when & then
        assertThatThrownBy(() -> socialLoginService.login("apple",
                new SocialTokenLoginRequest(APPLE_IDENTITY_TOKEN, AUTHORIZATION_CODE)))
                .isInstanceOf(SocialTokenInvalidException.class);
        verify(socialUserService, never()).findOrCreateAppleUser(any(), any());
        verify(authService, never()).issueTokens(any());
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_code_id_token_has_no_subject() {
        // given
        given(appleVerifier.verify(APPLE_IDENTITY_TOKEN)).willReturn(appleUserInfo);
        given(appleAuthClient.exchangeAuthorizationCode(AUTHORIZATION_CODE))
                .willReturn(new AppleTokens(APPLE_REFRESH_TOKEN, APPLE_CODE_ID_TOKEN));
        given(appleVerifier.extractSubject(APPLE_CODE_ID_TOKEN)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> socialLoginService.login("apple",
                new SocialTokenLoginRequest(APPLE_IDENTITY_TOKEN, AUTHORIZATION_CODE)))
                .isInstanceOf(SocialTokenInvalidException.class);
        verify(socialUserService, never()).findOrCreateAppleUser(any(), any());
        verify(authService, never()).issueTokens(any());
    }

    // -------------------------------------------------------------------------
    // 입력 검증
    // -------------------------------------------------------------------------

    @Test
    void should_throw_InvalidInputException_when_provider_is_not_supported() {
        // given
        SocialTokenLoginRequest request = new SocialTokenLoginRequest("naver-token", null);

        // when & then
        assertThatThrownBy(() -> socialLoginService.login("naver", request))
                .isInstanceOf(InvalidInputException.class);
        verify(googleVerifier, never()).verify(anyString());
        verify(appleVerifier, never()).verify(anyString());
    }

    @Test
    void should_throw_InvalidInputException_when_apple_login_without_authorization_code() {
        // given
        SocialTokenLoginRequest request = new SocialTokenLoginRequest(APPLE_IDENTITY_TOKEN, null);

        // when & then
        assertThatThrownBy(() -> socialLoginService.login("apple", request))
                .isInstanceOf(InvalidInputException.class);
        verify(appleVerifier, never()).verify(anyString());
        verifyNoInteractions(appleAuthClient);
    }

    @Test
    void should_throw_InvalidInputException_when_apple_login_with_blank_authorization_code() {
        // given
        SocialTokenLoginRequest request = new SocialTokenLoginRequest(APPLE_IDENTITY_TOKEN, "");

        // when & then
        assertThatThrownBy(() -> socialLoginService.login("apple", request))
                .isInstanceOf(InvalidInputException.class);
        verify(appleVerifier, never()).verify(anyString());
        verifyNoInteractions(appleAuthClient);
    }

    // -------------------------------------------------------------------------
    // 예외 전파
    // -------------------------------------------------------------------------

    @Test
    void should_propagate_SocialTokenInvalidException_and_not_create_session_when_token_verification_fails() {
        // given
        given(googleVerifier.verify(GOOGLE_ID_TOKEN)).willThrow(new SocialTokenInvalidException());

        // when & then
        assertThatThrownBy(() -> socialLoginService.login("google",
                new SocialTokenLoginRequest(GOOGLE_ID_TOKEN, null)))
                .isInstanceOf(SocialTokenInvalidException.class);
        verify(socialUserService, never()).findOrCreate(anyString(), any());
        verify(authService, never()).issueTokens(any());
    }

    @Test
    void should_propagate_UserSuspendedException_and_not_create_session_when_user_is_suspended() {
        // given
        given(googleVerifier.verify(GOOGLE_ID_TOKEN)).willReturn(googleUserInfo);
        given(socialUserService.findOrCreate("google", googleUserInfo)).willThrow(new UserSuspendedException());

        // when & then
        assertThatThrownBy(() -> socialLoginService.login("google",
                new SocialTokenLoginRequest(GOOGLE_ID_TOKEN, null)))
                .isInstanceOf(UserSuspendedException.class);
        verify(authService, never()).issueTokens(any());
    }
}
