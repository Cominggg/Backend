package com.Coming.Backend.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.auth.exception.UserSuspendedException;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.report.entity.ReportReason;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2FailureHandlerTest {

    private OAuth2FailureHandler oAuth2FailureHandler;

    @BeforeEach
    void setUp() {
        oAuth2FailureHandler = new OAuth2FailureHandler();
        ReflectionTestUtils.setField(oAuth2FailureHandler, "redirectBaseUri",
                "http://localhost:3000/auth/callback");
    }

    @Test
    void should_redirect_with_OAUTH2_FAILED_when_generic_authentication_failure() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new AuthenticationException("OAuth2 인증 실패") {};

        // when
        oAuth2FailureHandler.onAuthenticationFailure(request, response, exception);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/auth/callback?error=OAUTH2_FAILED");
    }

    @Test
    void should_redirect_with_USER_SUSPENDED_when_suspended_user_attempts_login() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error(ErrorCode.USER_SUSPENDED.name()), "정지된 계정입니다.");

        // when
        oAuth2FailureHandler.onAuthenticationFailure(request, response, exception);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/auth/callback?error=USER_SUSPENDED");
    }

    @Test
    void should_redirect_with_suspended_until_and_reason_when_suspension_cause_is_given() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserSuspendedException cause = new UserSuspendedException(
                LocalDateTime.of(2026, 11, 5, 12, 0, 0, 500_000_000), ReportReason.SPAM);
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error(ErrorCode.USER_SUSPENDED.name()), "정지된 계정입니다.", cause);

        // when
        oAuth2FailureHandler.onAuthenticationFailure(request, response, exception);

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo(
                "http://localhost:3000/auth/callback?error=USER_SUSPENDED&suspendedUntil=2026-11-05T12:00:00&reason=SPAM");
    }

    @Test
    void should_omit_suspended_until_when_suspension_is_permanent() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserSuspendedException cause = new UserSuspendedException(null, ReportReason.ILLEGAL);
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error(ErrorCode.USER_SUSPENDED.name()), "정지된 계정입니다.", cause);

        // when
        oAuth2FailureHandler.onAuthenticationFailure(request, response, exception);

        // then
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/auth/callback?error=USER_SUSPENDED&reason=ILLEGAL");
    }
}
