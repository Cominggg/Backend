package com.Coming.Backend.policy.batch;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.exception.PolicyNotFoundException;
import com.Coming.Backend.policy.repository.PolicyDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PENDING 상태의 PolicyNotificationTarget에 대해 정책 변경 고지 메일을 발송하고 상태(SENT/FAILED)를 갱신한다.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class PolicyNotificationMailProcessor implements ItemProcessor<PolicyNotificationTarget, PolicyNotificationTarget> {

    private final UserRepository userRepository;
    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyNotificationSender policyNotificationSender;

    @Value("#{jobParameters['policyId']}")
    private Long policyId;

    private PolicyDocument policyDocument;

    @Override
    public PolicyNotificationTarget process(PolicyNotificationTarget target) {
        User user = userRepository.findById(target.getUserId()).orElse(null);
        policyNotificationSender.sendAndMark(target, user, resolvePolicyDocument());
        return target;
    }

    private PolicyDocument resolvePolicyDocument() {
        if (policyDocument == null) {
            policyDocument = policyDocumentRepository.findById(policyId).orElseThrow(PolicyNotFoundException::new);
        }
        return policyDocument;
    }
}
