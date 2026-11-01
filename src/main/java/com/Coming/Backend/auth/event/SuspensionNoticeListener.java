package com.Coming.Backend.auth.event;

import com.Coming.Backend.auth.mail.SuspensionNoticeMailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 정지 트랜잭션이 커밋된 뒤 정지 통지 메일을 보낸다. 실패해도 정지는 유지하고 WARN 로그만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuspensionNoticeListener {

    private final SuspensionNoticeMailSender suspensionNoticeMailSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSuspensionNotice(SuspensionNoticeEvent event) {
        try {
            suspensionNoticeMailSender.send(event);
        } catch (RuntimeException e) {
            log.warn("정지 통지 메일 발송 실패 — userId: {}, error: {}", event.userId(), e.getMessage());
        }
    }
}
