package com.Coming.Backend.auth.event;

import com.Coming.Backend.auth.service.SocialUserService;
import com.Coming.Backend.auth.social.AppleAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 탈퇴 트랜잭션이 커밋된 뒤 Apple 토큰을 revoke한다. 성공했을 때만 저장된 토큰을 지운다.
 * 실패하면 토큰을 남겨 두지만 자동 재시도는 하지 않는다(WARN 로그로 확인 후 수동 처리).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleTokenRevokeListener {

    private final AppleAuthClient appleAuthClient;
    private final SocialUserService socialUserService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppleTokenRevoke(AppleTokenRevokeEvent event) {
        try {
            appleAuthClient.revoke(event.appleRefreshToken());
        } catch (RuntimeException e) {
            log.warn("Apple 토큰 revoke 실패 — 토큰 유지(수동 재시도 필요), userId: {}, error: {}", event.userId(), e.getMessage());
            return;
        }
        socialUserService.clearAppleRefreshToken(event.userId());
    }
}
