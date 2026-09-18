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
 * 정책 등록 트랜잭션이 커밋된 후, 해당 정책에 대한 알림 대상 생성·메일 발송 배치를 실행한다.
 * AFTER_COMMIT 콜백과 같은 스레드에서 배치(자체 트랜잭션 포함)를 직접 실행하면 커밋 중이던
 * 트랜잭션 동기화 상태와 충돌해 JobInterruptedException이 발생하므로, 별도 스레드에서 실행하고
 * join으로 대기해 호출자 입장에서는 동기로 완료를 기다리게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyNotificationJobTrigger {

    private final JobOperator jobOperator;
    private final Job policyNotificationJob;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPolicyRegistered(PolicyRegisteredEvent event) {
        Thread jobThread = Thread.ofVirtual().start(() -> runJob(event.policyId()));
        try {
            jobThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("정책 알림 배치 대기 중 인터럽트 발생 — policyId: {}", event.policyId(), e);
        }
    }

    private void runJob(Long policyId) {
        try {
            JobParameters jobParameters = policyNotificationJob.getJobParametersIncrementer().getNext(
                    new JobParametersBuilder()
                            .addLong("policyId", policyId)
                            .toJobParameters()
            );
            jobOperator.run(policyNotificationJob, jobParameters);
        } catch (Exception e) {
            log.error("정책 알림 배치 실행 실패 — policyId: {}", policyId, e);
        }
    }
}
