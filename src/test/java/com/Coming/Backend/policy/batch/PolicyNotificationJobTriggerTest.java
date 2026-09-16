package com.Coming.Backend.policy.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.policy.event.PolicyRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.launch.JobOperator;

@ExtendWith(MockitoExtension.class)
class PolicyNotificationJobTriggerTest {

    @InjectMocks
    private PolicyNotificationJobTrigger policyNotificationJobTrigger;

    @Mock
    private JobOperator jobOperator;

    @Mock
    private Job policyNotificationJob;

    @Test
    void should_run_job_with_policy_id_when_policy_registered_event_given() throws Exception {
        // given
        given(policyNotificationJob.getJobParametersIncrementer()).willReturn(new RunIdIncrementer());
        PolicyRegisteredEvent event = new PolicyRegisteredEvent(1L);

        // when
        policyNotificationJobTrigger.onPolicyRegistered(event);

        // then
        ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobOperator).run(eq(policyNotificationJob), jobParametersCaptor.capture());
        assertThat(jobParametersCaptor.getValue().getLong("policyId")).isEqualTo(1L);
    }

    @Test
    void should_not_throw_exception_when_job_operator_run_throws_exception() throws Exception {
        // given
        given(policyNotificationJob.getJobParametersIncrementer()).willReturn(new RunIdIncrementer());
        willThrow(new RuntimeException("job launch failed")).given(jobOperator).run(any(Job.class), any(JobParameters.class));
        PolicyRegisteredEvent event = new PolicyRegisteredEvent(1L);

        // when & then
        assertThatCode(() -> policyNotificationJobTrigger.onPolicyRegistered(event))
                .doesNotThrowAnyException();
    }
}
