package com.Coming.Backend.auth.controller;

import com.Coming.Backend.auth.dto.LoginNonceResponse;
import com.Coming.Backend.auth.dto.SocialLoginResponse;
import com.Coming.Backend.auth.dto.SocialTokenLoginRequest;
import com.Coming.Backend.auth.service.SocialLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth/login")
@RequiredArgsConstructor
public class SocialLoginController {

    private final SocialLoginService socialLoginService;

    @Operation(summary = "앱 소셜 로그인용 nonce 발급")
    @PostMapping("/nonce")
    public ResponseEntity<LoginNonceResponse> issueLoginNonce() {
        return ResponseEntity.ok(socialLoginService.issueNonce());
    }

    @Operation(summary = "앱 소셜 토큰 로그인")
    @ApiResponse(responseCode = "400", description = "지원하지 않는 provider 또는 apple인데 authorizationCode 누락")
    @ApiResponse(responseCode = "401", description = "유효하지 않은 소셜 로그인 토큰, nonce 불일치·재사용")
    @ApiResponse(responseCode = "403", description = "정지된 계정")
    @ApiResponse(responseCode = "502", description = "소셜 로그인 제공자 장애")
    @PostMapping("/{provider}/token")
    public ResponseEntity<SocialLoginResponse> loginWithSocialToken(
            @PathVariable String provider,
            @Valid @RequestBody SocialTokenLoginRequest request) {
        return ResponseEntity.ok(socialLoginService.login(provider, request));
    }
}
