package com.Coming.Backend.auth.event;

public record AppleTokenRevokeEvent(Long userId, String appleRefreshToken) {
}
