package com.Coming.Backend.auth.event;

public record KakaoUnlinkEvent(Long userId, String kakaoUserId) {
}
