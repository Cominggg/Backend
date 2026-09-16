package com.Coming.Backend.policy.batch;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.exception.PolicyNotFoundException;
import com.Coming.Backend.policy.mail.PolicyNoticeMailSender;
import com.Coming.Backend.policy.repository.PolicyDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PENDING 상태의 PolicyNotificationTarget에 대해 정책 변경 고지 메일을 발송하고 상태(SENT/FAILED)를 갱신한다.
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PolicyNotificationMailProcessor implements ItemProcessor<PolicyNotificationTarget, PolicyNotificationTarget> {

    private final UserRepository userRepository;
    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyNoticeMailSender policyNoticeMailSender;

    @Value("#{jobParameters['policyId']}")
    private Long policyId;

    private PolicyDocument policyDocument;

    @Override
    public PolicyNotificationTarget process(PolicyNotificationTarget target) {
        User user = userRepository.findById(target.getUserId()).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("수신 이메일이 없어 발송 실패 처리 — targetId: {}", target.getId());
            target.markFailed();
            return target;
        }
        try {
            policyNoticeMailSender.send(user.getEmail(), resolvePolicyDocument());
            target.markSent();
        } catch (Exception e) {
            log.warn("정책 변경 고지 메일 발송 실패 — targetId: {}", target.getId(), e);
            target.markFailed();
        }
        return target;
    }

    private PolicyDocument resolvePolicyDocument() {
        if (policyDocument == null) {
            policyDocument = policyDocumentRepository.findById(policyId).orElseThrow(PolicyNotFoundException::new);
        }
        return policyDocument;
    }
}
