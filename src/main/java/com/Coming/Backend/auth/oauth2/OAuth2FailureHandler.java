package com.Coming.Backend.auth.oauth2;

import com.Coming.Backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-base-uri}")
    private String redirectBaseUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        log.warn("OAuth2 로그인 실패: {}", exception.getMessage());

        String errorParam = "OAUTH2_FAILED";
        if (exception instanceof OAuth2AuthenticationException oAuth2Ex
                && oAuth2Ex.getError() != null
                && ErrorCode.USER_SUSPENDED.name().equals(oAuth2Ex.getError().getErrorCode())) {
            errorParam = ErrorCode.USER_SUSPENDED.name();
        }

        response.sendRedirect(redirectBaseUri + "?error=" + errorParam);
    }
}
