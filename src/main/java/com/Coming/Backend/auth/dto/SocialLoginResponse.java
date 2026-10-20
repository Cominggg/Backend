package com.Coming.Backend.auth.dto;

public record SocialLoginResponse(String accessToken, String refreshToken, boolean isNewUser) {
}
