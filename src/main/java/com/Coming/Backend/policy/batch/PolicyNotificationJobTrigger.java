package com.Coming.Backend.policy.batch;

import com.Coming.Backend.policy.event.PolicyRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 정책 등록 트랜잭션이 커밋된 후, 해당 정책에 대한 알림 대상 생성·메일 발송 배치를 동기로 실행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyNotificationJobTrigger {

    private final JobOperator jobOperator;
    private final Job policyNotificationJob;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPolicyRegistered(PolicyRegisteredEvent event) {
        try {
            JobParameters jobParameters = policyNotificationJob.getJobParametersIncrementer().getNext(
                    new JobParametersBuilder()
                            .addLong("policyId", event.policyId())
                            .toJobParameters()
            );
            jobOperator.start(policyNotificationJob, jobParameters);
        } catch (Exception e) {
            log.error("정책 알림 배치 실행 실패 — policyId: {}", event.policyId(), e);
        }
    }
}
