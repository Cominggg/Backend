package com.Coming.Backend.policy.repository;

import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyNotificationTargetRepository extends JpaRepository<PolicyNotificationTarget, Long> {

    List<PolicyNotificationTarget> findByPolicyId(Long policyId);

    Page<PolicyNotificationTarget> findByPolicyIdAndStatus(Long policyId, NotificationStatus status, Pageable pageable);

    List<PolicyNotificationTarget> findByStatusAndRetryCountLessThan(NotificationStatus status, int retryCount);
}
