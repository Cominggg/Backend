package com.Coming.Backend.auth.event;

import com.Coming.Backend.auth.social.KakaoUnlinkClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 탈퇴 트랜잭션이 커밋된 뒤 Kakao 연결 끊기를 요청한다.
 * 실패해도 탈퇴는 유지하고 자동 재시도는 하지 않는다(WARN 로그로 확인 후 수동 처리).
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
            log.warn("Kakao 연결 끊기 실패(수동 처리 필요) — userId: {}, kakaoUserId: {}, error: {}",
                    event.userId(), event.kakaoUserId(), e.getMessage());
        }
    }
}
