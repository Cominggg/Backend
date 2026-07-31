package com.Coming.Backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.Coming.Backend.auth.jwt.JwtProvider;
import com.Coming.Backend.auth.oauth2.CustomOAuth2UserService;
import com.Coming.Backend.auth.oauth2.OAuth2FailureHandler;
import com.Coming.Backend.auth.oauth2.OAuth2SuccessHandler;
import com.Coming.Backend.auth.repository.BlacklistRepository;
import com.Coming.Backend.common.discord.DiscordNotifier;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private BlacklistRepository blacklistRepository;

    @Mock
    private CustomOAuth2UserService oAuth2UserService;

    @Mock
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Mock
    private OAuth2FailureHandler oAuth2FailureHandler;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Environment environment;

    @Mock
    private ProxyManager<String> rateLimitProxyManager;

    @Mock
    private DiscordNotifier discordNotifier;

    private SecurityConfig securityConfig;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtProvider, blacklistRepository, oAuth2UserService,
                oAuth2SuccessHandler, oAuth2FailureHandler, objectMapper, environment,
                rateLimitProxyManager, discordNotifier);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void should_return401WithoutDiscordAlert_when_apiPathUnauthenticated() throws Exception {
        // given
        request.setRequestURI("/api/auth/me");
        request.setMethod("GET");

        // when
        securityConfig.handleAuthenticationFailure(request, response);

        // then
        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(discordNotifier);
    }

    @Test
    void should_return404_when_nonApiPathUnauthenticated() throws Exception {
        // given
        request.setRequestURI("/some-page");

        // when
        securityConfig.handleAuthenticationFailure(request, response);

        // then
        assertThat(response.getStatus()).isEqualTo(404);
        verifyNoInteractions(discordNotifier);
    }

    @Test
    void should_return403WithoutDiscordAlert_when_apiPathAccessDenied() throws Exception {
        // given
        request.setRequestURI("/api/admin/users");

        // when
        securityConfig.handleAccessDenied(request, response);

        // then
        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(discordNotifier);
    }

    @Test
    void should_return404_when_nonApiPathAccessDenied() throws Exception {
        // given
        request.setRequestURI("/some-page");

        // when
        securityConfig.handleAccessDenied(request, response);

        // then
        assertThat(response.getStatus()).isEqualTo(404);
        verifyNoInteractions(discordNotifier);
    }
}
