package com.Coming.Backend.policy.repository;

import com.Coming.Backend.policy.entity.UserPolicyAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPolicyAgreementRepository extends JpaRepository<UserPolicyAgreement, Long> {

    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);
}
