package com.Coming.Backend.policy.batch;

import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.repository.PolicyNotificationTargetRepository;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * PENDING 상태의 정책 알림 대상을 id 오름차순 커서 기반으로 읽는다.
 * offset 페이징을 사용하면 처리된 항목이 PENDING에서 벗어나며 결과 집합이 줄어들어
 * 다음 페이지 조회 시 대상을 건너뛰는 문제가 있어, 마지막으로 읽은 id를 커서로 다음 배치를 조회한다.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class PolicyNotificationPendingTargetReader implements ItemReader<PolicyNotificationTarget> {

    private static final int PAGE_SIZE = 20;

    private final PolicyNotificationTargetRepository policyNotificationTargetRepository;

    @Value("#{jobParameters['policyId']}")
    private Long policyId;

    private Long lastId = 0L;
    private Iterator<PolicyNotificationTarget> currentBatch = List.<PolicyNotificationTarget>of().iterator();

    @Override
    public PolicyNotificationTarget read() {
        if (!currentBatch.hasNext()) {
            List<PolicyNotificationTarget> batch = policyNotificationTargetRepository
                    .findByPolicyIdAndStatusAndIdGreaterThanOrderByIdAsc(
                            policyId, NotificationStatus.PENDING, lastId, PageRequest.of(0, PAGE_SIZE));
            if (batch.isEmpty()) {
                return null;
            }
            lastId = batch.get(batch.size() - 1).getId();
            currentBatch = batch.iterator();
        }
        return currentBatch.next();
    }
}
