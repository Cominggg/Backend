package com.Coming.Backend.policy.batch;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.repository.PolicyNotificationTargetRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * 정책 발행 시 ACTIVE 유저 중 아직 발송 대상이 아닌 유저를 PENDING 상태의 PolicyNotificationTarget으로 등록한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyNotificationTargetCreationTasklet implements Tasklet {

    private final UserRepository userRepository;
    private final PolicyNotificationTargetRepository policyNotificationTargetRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Long policyId = chunkContext.getStepContext().getStepExecution().getJobParameters().getLong("policyId");

        Set<Long> existingTargetUserIds = policyNotificationTargetRepository.findByPolicyId(policyId).stream()
                .map(PolicyNotificationTarget::getUserId)
                .collect(Collectors.toSet());

        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
        int created = 0;
        for (User user : activeUsers) {
            if (existingTargetUserIds.contains(user.getId())) {
                continue;
            }
            if (user.hasNoEmail()) {
                log.warn("이메일이 없어 정책 알림 대상에서 제외 — userId: {}", user.getId());
                continue;
            }
            policyNotificationTargetRepository.save(
                    PolicyNotificationTarget.builder()
                            .policyId(policyId)
                            .userId(user.getId())
                            .status(NotificationStatus.PENDING)
                            .retryCount(0)
                            .build()
            );
            created++;
        }
        log.info("정책 알림 대상 생성 완료 — policyId: {}, 생성 건수: {}", policyId, created);
        return RepeatStatus.FINISHED;
    }
}
