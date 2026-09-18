package com.Coming.Backend.policy.dto;

import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyType;
import java.time.LocalDate;

public record PolicyResponse(
        Long id,
        PolicyType type,
        String version,
        LocalDate effectiveDate,
        String changeSummary,
        String detailUrl
) {
    public static PolicyResponse from(PolicyDocument policyDocument) {
        return new PolicyResponse(
                policyDocument.getId(),
                policyDocument.getType(),
                policyDocument.getVersion(),
                policyDocument.getEffectiveDate(),
                policyDocument.getChangeSummary(),
                policyDocument.getDetailUrl()
        );
    }
}
