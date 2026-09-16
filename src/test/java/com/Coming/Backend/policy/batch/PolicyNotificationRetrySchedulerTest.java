package com.Coming.Backend.policy.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.entity.PolicyType;
import com.Coming.Backend.policy.repository.PolicyDocumentRepository;
import com.Coming.Backend.policy.repository.PolicyNotificationTargetRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyNotificationRetrySchedulerTest {

    @InjectMocks
    private PolicyNotificationRetryScheduler policyNotificationRetryScheduler;

    @Mock
    private PolicyNotificationTargetRepository policyNotificationTargetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PolicyDocumentRepository policyDocumentRepository;

    @Mock
    private PolicyNotificationSender policyNotificationSender;

    private static final Long POLICY_ID = 1L;

    private User buildUser(Long userId, String email) {
        return User.builder()
                .id(userId)
                .email(email)
                .provider("google")
                .providerId("provider-" + userId)
                .nickname("nickname" + userId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .birthYear(1995)
                .agreedMarketing(true)
                .build();
    }

    private PolicyDocument buildPolicyDocument() {
        return PolicyDocument.builder()
                .id(POLICY_ID)
                .type(PolicyType.TERMS)
                .version("1.0")
                .effectiveDate(LocalDate.of(2026, 1, 1))
                .changeSummary("이용약관 개정")
                .detailUrl("https://coming.com/policy/terms/1.0")
                .requiresReconsent(false)
                .build();
    }

    private PolicyNotificationTarget buildTarget(Long userId) {
        return PolicyNotificationTarget.builder()
                .id(10L)
                .policyId(POLICY_ID)
                .userId(userId)
                .status(NotificationStatus.FAILED)
                .retryCount(1)
                .build();
    }

    @Test
    void should_do_nothing_when_no_retry_targets_given() {
        // given
        given(policyNotificationTargetRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3))
                .willReturn(Collections.emptyList());

        // when
        policyNotificationRetryScheduler.retryFailedNotifications();

        // then
        verifyNoInteractions(userRepository, policyDocumentRepository, policyNotificationSender);
    }

    @Test
    void should_mark_failed_without_calling_sender_when_policy_document_not_found_given() {
        // given
        PolicyNotificationTarget target = buildTarget(1L);
        User user = buildUser(1L, "iu@coming.com");

        given(policyNotificationTargetRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3))
                .willReturn(List.of(target));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(policyDocumentRepository.findById(POLICY_ID)).willReturn(Optional.empty());

        // when
        policyNotificationRetryScheduler.retryFailedNotifications();

        // then
        assertThat(target.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(target.getRetryCount()).isEqualTo(2);
        verify(policyNotificationSender, never()).sendAndMark(any(), any(), any());
    }

    @Test
    void should_call_sender_with_found_user_and_policy_document_when_both_exist_given() {
        // given
        PolicyNotificationTarget target = buildTarget(1L);
        User user = buildUser(1L, "iu@coming.com");
        PolicyDocument policyDocument = buildPolicyDocument();

        given(policyNotificationTargetRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3))
                .willReturn(List.of(target));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(policyDocumentRepository.findById(POLICY_ID)).willReturn(Optional.of(policyDocument));

        // when
        policyNotificationRetryScheduler.retryFailedNotifications();

        // then
        verify(policyNotificationSender).sendAndMark(target, user, policyDocument);
    }

    @Test
    void should_call_sender_with_null_user_when_user_not_found_but_policy_document_found_given() {
        // given
        PolicyNotificationTarget target = buildTarget(1L);
        PolicyDocument policyDocument = buildPolicyDocument();

        given(policyNotificationTargetRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3))
                .willReturn(List.of(target));
        given(userRepository.findById(1L)).willReturn(Optional.empty());
        given(policyDocumentRepository.findById(POLICY_ID)).willReturn(Optional.of(policyDocument));

        // when
        policyNotificationRetryScheduler.retryFailedNotifications();

        // then
        verify(policyNotificationSender).sendAndMark(target, null, policyDocument);
    }
}
