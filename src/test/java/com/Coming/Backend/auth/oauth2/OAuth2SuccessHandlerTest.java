package com.Coming.Backend.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.jwt.JwtProvider;
import com.Coming.Backend.auth.repository.TokenRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @InjectMocks
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private Authentication authentication;

    private static final long REFRESH_TOKEN_EXPIRY = 604_800_000L;
    private static final String REDIRECT_BASE_URI = "http://localhost:3000/auth/callback";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuth2SuccessHandler, "refreshTokenExpiry", REFRESH_TOKEN_EXPIRY);
        ReflectionTestUtils.setField(oAuth2SuccessHandler, "redirectBaseUri", REDIRECT_BASE_URI);
    }

    private User buildUser(UserRole role) {
        User user = User.builder()
                .provider("google")
                .providerId("google-123")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    @Test
    void should_redirect_with_isNewUser_false_when_existing_user_logs_in() throws Exception {
        // given
        CustomOAuth2User oAuth2User = new CustomOAuth2User(buildUser(UserRole.USER), Map.of(), false);
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token-value");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_BASE_URI + "?isNewUser=false");
    }

    @Test
    void should_redirect_with_isNewUser_true_when_new_user_logs_in() throws Exception {
        // given
        CustomOAuth2User oAuth2User = new CustomOAuth2User(buildUser(UserRole.PENDING), Map.of(), true);
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token-value");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_BASE_URI + "?isNewUser=true");
    }

    @Test
    void should_redirect_with_isNewUser_true_when_returning_pending_user_logs_in() throws Exception {
        // given — isNewUser 플래그가 false여도 role이 PENDING이면 isNewUser=true
        CustomOAuth2User oAuth2User = new CustomOAuth2User(buildUser(UserRole.PENDING), Map.of(), false);
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token-value");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_BASE_URI + "?isNewUser=true");
    }

    @Test
    void should_set_httpOnly_refresh_token_cookie_when_authentication_success() throws Exception {
        // given
        CustomOAuth2User oAuth2User = new CustomOAuth2User(buildUser(UserRole.USER), Map.of(), false);
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token-value");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Lax");
        assertThat(setCookie).contains("refresh-token-value");
    }

    @Test
    void should_call_tokenRepository_save_when_authentication_success() throws Exception {
        // given
        CustomOAuth2User oAuth2User = new CustomOAuth2User(buildUser(UserRole.USER), Map.of(), false);
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token-value");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(tokenRepository).save(1L, "refresh-token-value", REFRESH_TOKEN_EXPIRY);
    }
}
