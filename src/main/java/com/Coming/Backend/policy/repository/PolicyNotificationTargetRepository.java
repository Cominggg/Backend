package com.Coming.Backend.policy.repository;

import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyNotificationTargetRepository extends JpaRepository<PolicyNotificationTarget, Long> {

    List<PolicyNotificationTarget> findByPolicyId(Long policyId);

    List<PolicyNotificationTarget> findByPolicyIdAndStatusAndIdGreaterThanOrderByIdAsc(
            Long policyId, NotificationStatus status, Long id, Pageable pageable);

    List<PolicyNotificationTarget> findByStatusAndRetryCountLessThan(NotificationStatus status, int retryCount);
}
