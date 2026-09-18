package com.Coming.Backend.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.policy.dto.PolicyRegisterRequest;
import com.Coming.Backend.policy.dto.PolicyResponse;
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyType;
import com.Coming.Backend.policy.event.PolicyRegisteredEvent;
import com.Coming.Backend.policy.exception.PolicyVersionDuplicateException;
import com.Coming.Backend.policy.repository.PolicyDocumentRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @InjectMocks
    private PolicyService policyService;

    @Mock
    private PolicyDocumentRepository policyDocumentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final PolicyType POLICY_TYPE = PolicyType.TERMS;
    private static final String VERSION = "1.0.0";

    private PolicyRegisterRequest buildRequest() {
        return new PolicyRegisterRequest(
                POLICY_TYPE,
                VERSION,
                LocalDate.of(2026, 1, 1),
                "이용약관 최초 등록",
                "https://coming.example.com/policy/terms/1.0.0"
        );
    }

    private PolicyDocument buildPolicyDocument(Long id, PolicyRegisterRequest request) {
        return PolicyDocument.builder()
                .id(id)
                .type(request.type())
                .version(request.version())
                .effectiveDate(request.effectiveDate())
                .changeSummary(request.changeSummary())
                .detailUrl(request.detailUrl())
                .build();
    }

    @Test
    void should_return_policy_response_when_valid_request_given() {
        // given
        PolicyRegisterRequest request = buildRequest();
        PolicyDocument savedPolicyDocument = buildPolicyDocument(1L, request);

        given(policyDocumentRepository.existsByTypeAndVersion(POLICY_TYPE, VERSION)).willReturn(false);
        given(policyDocumentRepository.saveAndFlush(any(PolicyDocument.class))).willReturn(savedPolicyDocument);

        // when
        PolicyResponse result = policyService.registerPolicy(request);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.type()).isEqualTo(POLICY_TYPE);
        assertThat(result.version()).isEqualTo(VERSION);
        assertThat(result.effectiveDate()).isEqualTo(request.effectiveDate());
        assertThat(result.changeSummary()).isEqualTo(request.changeSummary());
        assertThat(result.detailUrl()).isEqualTo(request.detailUrl());
        verify(eventPublisher).publishEvent(new PolicyRegisteredEvent(1L));
    }

    @Test
    void should_throw_policy_version_duplicate_exception_when_same_type_and_version_already_exists() {
        // given
        PolicyRegisterRequest request = buildRequest();

        given(policyDocumentRepository.existsByTypeAndVersion(POLICY_TYPE, VERSION)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> policyService.registerPolicy(request))
                .isInstanceOf(PolicyVersionDuplicateException.class)
                .hasMessage(ErrorCode.POLICY_VERSION_DUPLICATE.getMessage());
        verify(policyDocumentRepository, never()).saveAndFlush(any(PolicyDocument.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void should_throw_policy_version_duplicate_exception_when_concurrent_insert_violates_unique_constraint() {
        // given
        PolicyRegisterRequest request = buildRequest();

        given(policyDocumentRepository.existsByTypeAndVersion(POLICY_TYPE, VERSION)).willReturn(false);
        willThrow(new DataIntegrityViolationException("duplicate key"))
                .given(policyDocumentRepository).saveAndFlush(any(PolicyDocument.class));

        // when & then
        assertThatThrownBy(() -> policyService.registerPolicy(request))
                .isInstanceOf(PolicyVersionDuplicateException.class)
                .hasMessage(ErrorCode.POLICY_VERSION_DUPLICATE.getMessage());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
