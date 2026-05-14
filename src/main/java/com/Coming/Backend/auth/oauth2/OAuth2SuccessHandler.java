package com.Coming.Backend.auth.oauth2;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.jwt.JwtProvider;
import com.Coming.Backend.auth.repository.TokenRepository;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7일 (초)

    private final JwtProvider jwtProvider;
    private final TokenRepository tokenRepository;
    private final ObjectMapper objectMapper;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    public OAuth2SuccessHandler(JwtProvider jwtProvider, TokenRepository tokenRepository,
            ObjectMapper objectMapper) {
        this.jwtProvider = jwtProvider;
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        log.info("OAuth2 로그인 성공 — userId: {}", user.getId());
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        tokenRepository.save(user.getId(), refreshToken, refreshTokenExpiry);
        addRefreshTokenCookie(response, refreshToken);
        writeAccessTokenResponse(response, accessToken);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void writeAccessTokenResponse(HttpServletResponse response, String accessToken)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("accessToken", accessToken));
    }
}
