package com.Coming.Backend.policy.batch;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.repository.PolicyDocumentRepository;
import com.Coming.Backend.policy.repository.PolicyNotificationTargetRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발송 실패(FAILED)한 정책 알림 대상을 최대 3회까지 1시간 간격으로 재시도한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyNotificationRetryScheduler {

    private static final int MAX_RETRY_COUNT = 3;

    private final PolicyNotificationTargetRepository policyNotificationTargetRepository;
    private final UserRepository userRepository;
    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyNotificationSender policyNotificationSender;

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void retryFailedNotifications() {
        List<PolicyNotificationTarget> retryTargets = policyNotificationTargetRepository
                .findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, MAX_RETRY_COUNT);
        if (retryTargets.isEmpty()) {
            return;
        }
        log.info("정책 알림 재시도 시작 — 대상 건수: {}", retryTargets.size());
        for (PolicyNotificationTarget target : retryTargets) {
            retryOne(target);
        }
    }

    private void retryOne(PolicyNotificationTarget target) {
        User user = userRepository.findById(target.getUserId()).orElse(null);
        PolicyDocument policyDocument = policyDocumentRepository.findById(target.getPolicyId()).orElse(null);
        if (policyDocument == null) {
            target.markFailed();
            return;
        }
        policyNotificationSender.sendAndMark(target, user, policyDocument);
    }
}
