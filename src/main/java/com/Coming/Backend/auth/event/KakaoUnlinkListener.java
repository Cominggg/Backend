package com.Coming.Backend.auth.event;

import com.Coming.Backend.auth.social.KakaoUnlinkClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 탈퇴 트랜잭션이 커밋된 뒤 Kakao 연결 끊기를 요청한다.
 * 실패해도 탈퇴는 유지하고 재시도하지 않는다(회원번호를 보관하지 않으므로 수동 재처리도 하지 않는다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoUnlinkListener {

    private final KakaoUnlinkClient kakaoUnlinkClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKakaoUnlink(KakaoUnlinkEvent event) {
        try {
            kakaoUnlinkClient.unlink(event.kakaoUserId());
        } catch (RuntimeException e) {
            // 탈퇴 시 즉시 파기 원칙에 따라 Kakao 회원번호는 로그에 남기지 않는다.
            log.warn("Kakao 연결 끊기 실패 — userId: {}, error: {}", event.userId(), e.getMessage());
        }
    }
}
