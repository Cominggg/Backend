package com.Coming.Backend.policy.batch;

import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class PolicyNotificationBatchConfig {

    private static final int CHUNK_SIZE = 20;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PolicyNotificationTargetCreationTasklet policyNotificationTargetCreationTasklet;
    private final PolicyNotificationPendingTargetReader policyNotificationPendingTargetReader;
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
                .reader(policyNotificationPendingTargetReader)
                .processor(policyNotificationMailProcessor)
                .writer(policyNotificationTargetWriter)
                .transactionManager(transactionManager)
                .build();
    }
}
