package com.Coming.Backend.policy.batch;

import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.repository.PolicyNotificationTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolicyNotificationTargetWriter implements ItemWriter<PolicyNotificationTarget> {

    private final PolicyNotificationTargetRepository policyNotificationTargetRepository;

    @Override
    public void write(Chunk<? extends PolicyNotificationTarget> chunk) {
        policyNotificationTargetRepository.saveAll(chunk.getItems());
    }
}
