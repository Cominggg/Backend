package com.Coming.Backend.policy.repository;

import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyNotificationTargetRepository extends JpaRepository<PolicyNotificationTarget, Long> {
}
