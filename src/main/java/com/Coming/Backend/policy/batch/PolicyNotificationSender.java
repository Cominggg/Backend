package com.Coming.Backend.policy.batch;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.mail.PolicyNoticeMailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 정책 알림 대상에게 메일 발송을 시도하고 결과에 따라 상태(SENT/FAILED)를 마킹한다.
 * 배치 Step2 Processor와 재시도 스케줄러가 공통으로 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyNotificationSender {

    private final PolicyNoticeMailSender policyNoticeMailSender;

    public void sendAndMark(PolicyNotificationTarget target, User user, PolicyDocument policyDocument) {
        if (user == null || user.hasNoEmail()) {
            log.warn("수신 이메일이 없어 발송 실패 처리 — targetId: {}", target.getId());
            target.markFailed();
            return;
        }
        try {
            policyNoticeMailSender.send(user.getEmail(), policyDocument);
            target.markSent();
        } catch (Exception e) {
            log.warn("정책 변경 고지 메일 발송 실패 — targetId: {}", target.getId(), e);
            target.markFailed();
        }
    }
}
