package com.Coming.Backend.policy.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.repository.PolicyNotificationTargetRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

@ExtendWith(MockitoExtension.class)
class PolicyNotificationTargetCreationTaskletTest {

    @InjectMocks
    private PolicyNotificationTargetCreationTasklet policyNotificationTargetCreationTasklet;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PolicyNotificationTargetRepository policyNotificationTargetRepository;

    private static final Long POLICY_ID = 1L;

    private User buildActiveUser(Long userId, String email) {
        return User.builder()
                .id(userId)
                .email(email)
                .provider("google")
                .providerId("provider-" + userId)
                .nickname("nickname" + userId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .birthYear(1995)
                .agreedMarketing(true)
                .build();
    }

    private ChunkContext buildChunkContext(Long policyId) {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("policyId", policyId)
                .toJobParameters();
        JobInstance jobInstance = new JobInstance(1L, "policyNotificationJob");
        JobExecution jobExecution = new JobExecution(1L, jobInstance, jobParameters);
        StepExecution stepExecution = new StepExecution(1L, "targetCreationStep", jobExecution);
        return new ChunkContext(new StepContext(stepExecution));
    }

    @Test
    void should_save_pending_target_when_active_user_with_email_and_no_existing_target_given() {
        // given
        ChunkContext chunkContext = buildChunkContext(POLICY_ID);
        StepContribution stepContribution = new StepContribution(chunkContext.getStepContext().getStepExecution());
        User user = buildActiveUser(1L, "iu@coming.com");

        given(policyNotificationTargetRepository.findByPolicyId(POLICY_ID)).willReturn(List.of());
        given(userRepository.findByStatus(UserStatus.ACTIVE)).willReturn(List.of(user));

        ArgumentCaptor<PolicyNotificationTarget> targetCaptor = ArgumentCaptor.forClass(PolicyNotificationTarget.class);

        // when
        policyNotificationTargetCreationTasklet.execute(stepContribution, chunkContext);

        // then
        verify(policyNotificationTargetRepository).save(targetCaptor.capture());
        PolicyNotificationTarget savedTarget = targetCaptor.getValue();
        assertThat(savedTarget.getPolicyId()).isEqualTo(POLICY_ID);
        assertThat(savedTarget.getUserId()).isEqualTo(1L);
        assertThat(savedTarget.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(savedTarget.getRetryCount()).isZero();
    }

    @Test
    void should_skip_user_when_target_already_exists_for_policy_given() {
        // given
        ChunkContext chunkContext = buildChunkContext(POLICY_ID);
        StepContribution stepContribution = new StepContribution(chunkContext.getStepContext().getStepExecution());
        User user = buildActiveUser(1L, "iu@coming.com");
        PolicyNotificationTarget existingTarget = PolicyNotificationTarget.builder()
                .id(10L)
                .policyId(POLICY_ID)
                .userId(1L)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        given(policyNotificationTargetRepository.findByPolicyId(POLICY_ID)).willReturn(List.of(existingTarget));
        given(userRepository.findByStatus(UserStatus.ACTIVE)).willReturn(List.of(user));

        // when
        policyNotificationTargetCreationTasklet.execute(stepContribution, chunkContext);

        // then
        verify(policyNotificationTargetRepository, never()).save(any(PolicyNotificationTarget.class));
    }

    @Test
    void should_skip_user_when_email_is_null_given() {
        // given
        ChunkContext chunkContext = buildChunkContext(POLICY_ID);
        StepContribution stepContribution = new StepContribution(chunkContext.getStepContext().getStepExecution());
        User user = buildActiveUser(1L, null);

        given(policyNotificationTargetRepository.findByPolicyId(POLICY_ID)).willReturn(List.of());
        given(userRepository.findByStatus(UserStatus.ACTIVE)).willReturn(List.of(user));

        // when
        policyNotificationTargetCreationTasklet.execute(stepContribution, chunkContext);

        // then
        verify(policyNotificationTargetRepository, never()).save(any(PolicyNotificationTarget.class));
    }

    @Test
    void should_return_finished_when_execute_completes_given() {
        // given
        ChunkContext chunkContext = buildChunkContext(POLICY_ID);
        StepContribution stepContribution = new StepContribution(chunkContext.getStepContext().getStepExecution());

        given(policyNotificationTargetRepository.findByPolicyId(POLICY_ID)).willReturn(List.of());
        given(userRepository.findByStatus(UserStatus.ACTIVE)).willReturn(List.of());

        // when
        RepeatStatus result = policyNotificationTargetCreationTasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
    }
}
