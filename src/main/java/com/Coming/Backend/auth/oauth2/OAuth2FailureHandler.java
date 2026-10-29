package com.Coming.Backend.auth.oauth2;

import com.Coming.Backend.auth.exception.UserSuspendedException;
import com.Coming.Backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    /** JSON 응답(UserSuspendedErrorResponse)과 같은 오프셋 없는 KST 형식 */
    private static final DateTimeFormatter SUSPENDED_UNTIL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Value("${app.oauth2.redirect-base-uri}")
    private String redirectBaseUri;

    /**
     * 실패 사유를 error 쿼리로 담아 프론트 콜백으로 리다이렉트한다.
     * 정지 회원이면 suspendedUntil(영구 정지면 생략)·reason을 함께 붙인다.
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        log.warn("OAuth2 로그인 실패: {}", exception.getMessage());

        UriComponentsBuilder redirect = UriComponentsBuilder.fromUriString(redirectBaseUri);
        if (exception instanceof OAuth2AuthenticationException oAuth2Ex
                && oAuth2Ex.getError() != null
                && ErrorCode.USER_SUSPENDED.name().equals(oAuth2Ex.getError().getErrorCode())) {
            redirect.queryParam("error", ErrorCode.USER_SUSPENDED.name());
            if (oAuth2Ex.getCause() instanceof UserSuspendedException suspended) {
                if (suspended.getSuspendedUntil() != null) {
                    redirect.queryParam("suspendedUntil", suspended.getSuspendedUntil().format(SUSPENDED_UNTIL_FORMAT));
                }
                redirect.queryParam("reason", suspended.getReason());
            }
        } else {
            redirect.queryParam("error", "OAUTH2_FAILED");
        }

        response.sendRedirect(redirect.encode().build().toUriString());
    }
}
