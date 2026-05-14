package com.Coming.Backend.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.jwt.JwtProvider;
import com.Coming.Backend.auth.repository.TokenRepository;
import tools.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final long REFRESH_TOKEN_EXPIRY = 604_800_000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuth2SuccessHandler, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(oAuth2SuccessHandler, "refreshTokenExpiry",
                REFRESH_TOKEN_EXPIRY);
    }

    @Test
    void should_write_accessToken_in_response_body_when_authentication_success() throws Exception {
        // given
        User user = User.builder()
                .provider("google")
                .providerId("google-123")
                .nickname("IU")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        CustomOAuth2User oAuth2User = new CustomOAuth2User(user, Map.of());
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.generateAccessToken(1L, "USER")).willReturn("access-token-value");
        given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token-value");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        String body = response.getContentAsString();
        assertThat(body).contains("accessToken");
        assertThat(body).contains("access-token-value");
    }

    @Test
    void should_set_httpOnly_and_sameSite_lax_cookie_when_authentication_success()
            throws Exception {
        // given
        User user = User.builder()
                .provider("google")
                .providerId("google-123")
                .nickname("IU")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        CustomOAuth2User oAuth2User = new CustomOAuth2User(user, Map.of());
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.generateAccessToken(1L, "USER")).willReturn("access-token-value");
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
        User user = User.builder()
                .provider("google")
                .providerId("google-123")
                .nickname("IU")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        CustomOAuth2User oAuth2User = new CustomOAuth2User(user, Map.of());
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(jwtProvider.generateAccessToken(1L, "USER")).willReturn("access-token-value");
        given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token-value");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(tokenRepository).save(1L, "refresh-token-value", REFRESH_TOKEN_EXPIRY);
    }
}
