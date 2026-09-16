package com.Coming.Backend.policy.batch;

import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.repository.PolicyNotificationTargetRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class PolicyNotificationBatchConfig {

    private static final int CHUNK_SIZE = 20;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PolicyNotificationTargetCreationTasklet policyNotificationTargetCreationTasklet;
    private final PolicyNotificationTargetRepository policyNotificationTargetRepository;
    private final PolicyNotificationMailProcessor policyNotificationMailProcessor;
    private final PolicyNotificationTargetWriter policyNotificationTargetWriter;

    @Bean
    public Job policyNotificationJob() {
        return new JobBuilder("policyNotificationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(targetCreationStep())
                .next(mailSendStep())
                .build();
    }

    @Bean
    public Step targetCreationStep() {
        return new StepBuilder("targetCreationStep", jobRepository)
                .tasklet(policyNotificationTargetCreationTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step mailSendStep() {
        return new StepBuilder("mailSendStep", jobRepository)
                .<PolicyNotificationTarget, PolicyNotificationTarget>chunk(CHUNK_SIZE)
                .reader(mailSendItemReader(null))
                .processor(policyNotificationMailProcessor)
                .writer(policyNotificationTargetWriter)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<PolicyNotificationTarget> mailSendItemReader(
            @Value("#{jobParameters['policyId']}") Long policyId) {
        return new RepositoryItemReaderBuilder<PolicyNotificationTarget>()
                .name("mailSendItemReader")
                .repository(policyNotificationTargetRepository)
                .methodName("findByPolicyIdAndStatus")
                .arguments(List.of(policyId, NotificationStatus.PENDING))
                .sorts(Map.of("id", Sort.Direction.ASC))
                .pageSize(CHUNK_SIZE)
                .build();
    }
}
