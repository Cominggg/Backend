package com.Coming.Backend.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.common.exception.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2FailureHandlerTest {

    private OAuth2FailureHandler oAuth2FailureHandler;

    @BeforeEach
    void setUp() {
        oAuth2FailureHandler = new OAuth2FailureHandler(new ObjectMapper());
    }

    @Test
    void should_return_401_when_authentication_failure() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new AuthenticationException("OAuth2 인증 실패") {};

        // when
        oAuth2FailureHandler.onAuthenticationFailure(request, response, exception);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void should_write_unauthorized_code_in_response_body_when_authentication_failure()
            throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new AuthenticationException("OAuth2 인증 실패") {};

        // when
        oAuth2FailureHandler.onAuthenticationFailure(request, response, exception);

        // then
        String body = response.getContentAsString();
        assertThat(body).contains("\"code\":\"UNAUTHORIZED\"");
        assertThat(body).contains(ErrorCode.UNAUTHORIZED.getMessage());
    }
}
