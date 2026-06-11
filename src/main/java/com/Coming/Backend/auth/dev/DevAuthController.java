package com.Coming.Backend.auth.dev;

import com.Coming.Backend.auth.dev.DevAuthService.DevTokenResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@Tag(name = "Dev")
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevAuthController {

    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    private final DevAuthService devAuthService;

    @Operation(summary = "[로컬 전용] 유저 생성 및 토큰 발급")
    @PostMapping("/login")
    public Map<String, String> devLogin(
            @RequestBody DevLoginRequest request,
            HttpServletResponse response
    ) {
        DevTokenResult result = devAuthService.login(request.nickname(), request.role());
        addRefreshTokenCookie(response, result.refreshToken());
        return Map.of("accessToken", result.accessToken());
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)  // localhost는 HTTP
                .path("/")
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
