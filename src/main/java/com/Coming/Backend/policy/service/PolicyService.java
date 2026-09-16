package com.Coming.Backend.policy.service;

import com.Coming.Backend.policy.dto.PolicyRegisterRequest;
import com.Coming.Backend.policy.dto.PolicyResponse;
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.event.PolicyRegisteredEvent;
import com.Coming.Backend.policy.exception.PolicyVersionDuplicateException;
import com.Coming.Backend.policy.repository.PolicyDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyDocumentRepository policyDocumentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 새 정책 버전을 등록한다. 등록 완료(커밋) 후 정책 알림 배치가 트리거된다.
     *
     * @throws PolicyVersionDuplicateException 동일 type·version이 이미 등록된 경우
     */
    @Transactional
    public PolicyResponse registerPolicy(PolicyRegisterRequest request) {
        if (policyDocumentRepository.existsByTypeAndVersion(request.type(), request.version())) {
            throw new PolicyVersionDuplicateException();
        }
        PolicyDocument policyDocument = PolicyDocument.builder()
                .type(request.type())
                .version(request.version())
                .effectiveDate(request.effectiveDate())
                .changeSummary(request.changeSummary())
                .detailUrl(request.detailUrl())
                .requiresReconsent(Boolean.TRUE.equals(request.requiresReconsent()))
                .build();
        PolicyDocument saved;
        try {
            saved = policyDocumentRepository.saveAndFlush(policyDocument);
        } catch (DataIntegrityViolationException e) {
            throw new PolicyVersionDuplicateException();
        }
        eventPublisher.publishEvent(new PolicyRegisteredEvent(saved.getId()));
        return PolicyResponse.from(saved);
    }
}
