package com.Coming.Backend.auth.controller;

import com.Coming.Backend.auth.dto.MeResponse;
import com.Coming.Backend.auth.dto.NicknameCheckResponse;
import com.Coming.Backend.auth.dto.RegisterRequest;
import com.Coming.Backend.auth.dto.TokenResponse;
import com.Coming.Backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @Operation(summary = "소셜 로그인 페이지로 리다이렉트")
    @GetMapping("/login/{provider}")
    public ResponseEntity<Void> login(@PathVariable String provider) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/" + provider))
                .build();
    }

    @Operation(summary = "Access Token 재발급")
    @ApiResponse(responseCode = "401", description = "Refresh Token 만료 또는 유효하지 않음")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authorizationHeader,
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response) {
        authService.logout(authorizationHeader.substring(BEARER_PREFIX.length()), userId);
        deleteRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @RequestHeader("Authorization") String authorizationHeader,
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response) {
        authService.withdraw(authorizationHeader.substring(BEARER_PREFIX.length()), userId);
        deleteRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(authService.getMe(userId));
    }

    @Operation(summary = "내 프로필 수정")
    @ApiResponse(responseCode = "400", description = "닉네임 20자 초과")
    @PutMapping("/me")
    public ResponseEntity<MeResponse> updateMe(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String nickname) {
        return ResponseEntity.ok(authService.updateMe(userId, nickname));
    }

    @Operation(summary = "회원가입 완료")
    @ApiResponse(responseCode = "400", description = "필수 약관 미동의 또는 닉네임 20자 초과")
    @ApiResponse(responseCode = "409", description = "닉네임 중복")
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(
            @AuthenticationPrincipal Long userId,
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(userId, request));
    }

    @Operation(summary = "닉네임 중복 검사")
    @GetMapping("/check-nickname")
    public ResponseEntity<NicknameCheckResponse> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(authService.checkNickname(nickname));
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
