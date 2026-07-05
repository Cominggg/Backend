package com.Coming.Backend.auth.oauth2;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.jwt.JwtProvider;
import com.Coming.Backend.auth.repository.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final TokenRepository tokenRepository;

    @Value("${app.oauth2.redirect-base-uri}")
    private String redirectBaseUri;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    public OAuth2SuccessHandler(JwtProvider jwtProvider, TokenRepository tokenRepository) {
        this.jwtProvider = jwtProvider;
        this.tokenRepository = tokenRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        log.info("OAuth2 로그인 성공 — userId: {}, isNewUser: {}", user.getId(), oAuth2User.isNewUser());

        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        tokenRepository.save(user.getId(), refreshToken, refreshTokenExpiry);
        addRefreshTokenCookie(response, refreshToken);

        String redirectUrl = redirectBaseUri + "?isNewUser=" + (user.getRole() == UserRole.PENDING);
        response.sendRedirect(redirectUrl);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(refreshTokenExpiry / 1000)
                .sameSite("Strict")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
