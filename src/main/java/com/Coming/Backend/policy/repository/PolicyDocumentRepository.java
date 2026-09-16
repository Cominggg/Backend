package com.Coming.Backend.policy.repository;

import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {

    boolean existsByTypeAndVersion(PolicyType type, String version);
}
