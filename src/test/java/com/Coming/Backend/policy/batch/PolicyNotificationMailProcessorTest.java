package com.Coming.Backend.policy.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.entity.PolicyType;
import com.Coming.Backend.policy.exception.PolicyNotFoundException;
import com.Coming.Backend.policy.repository.PolicyDocumentRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PolicyNotificationMailProcessorTest {

    @InjectMocks
    private PolicyNotificationMailProcessor policyNotificationMailProcessor;

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
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
    }

    @Test
    void should_call_sender_with_found_user_when_user_exists_given() {
        // given
        ReflectionTestUtils.setField(policyNotificationMailProcessor, "policyId", POLICY_ID);
        PolicyNotificationTarget target = buildTarget(1L);
        User user = buildUser(1L, "iu@coming.com");
        PolicyDocument policyDocument = buildPolicyDocument();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(policyDocumentRepository.findById(POLICY_ID)).willReturn(Optional.of(policyDocument));

        ArgumentCaptor<PolicyNotificationTarget> targetCaptor = ArgumentCaptor.forClass(PolicyNotificationTarget.class);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<PolicyDocument> policyDocumentCaptor = ArgumentCaptor.forClass(PolicyDocument.class);

        // when
        PolicyNotificationTarget result = policyNotificationMailProcessor.process(target);

        // then
        assertThat(result).isSameAs(target);
        verify(policyNotificationSender).sendAndMark(targetCaptor.capture(), userCaptor.capture(), policyDocumentCaptor.capture());
        assertThat(targetCaptor.getValue()).isSameAs(target);
        assertThat(userCaptor.getValue()).isSameAs(user);
        assertThat(policyDocumentCaptor.getValue()).isSameAs(policyDocument);
    }

    @Test
    void should_call_sender_with_null_user_when_user_not_found_given() {
        // given
        ReflectionTestUtils.setField(policyNotificationMailProcessor, "policyId", POLICY_ID);
        PolicyNotificationTarget target = buildTarget(1L);
        PolicyDocument policyDocument = buildPolicyDocument();

        given(userRepository.findById(1L)).willReturn(Optional.empty());
        given(policyDocumentRepository.findById(POLICY_ID)).willReturn(Optional.of(policyDocument));

        // when
        policyNotificationMailProcessor.process(target);

        // then
        verify(policyNotificationSender).sendAndMark(target, null, policyDocument);
    }

    @Test
    void should_call_policy_document_repository_only_once_when_process_called_twice_given() {
        // given
        ReflectionTestUtils.setField(policyNotificationMailProcessor, "policyId", POLICY_ID);
        User user = buildUser(1L, "iu@coming.com");
        PolicyDocument policyDocument = buildPolicyDocument();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(policyDocumentRepository.findById(POLICY_ID)).willReturn(Optional.of(policyDocument));

        // when
        policyNotificationMailProcessor.process(buildTarget(1L));
        policyNotificationMailProcessor.process(buildTarget(1L));

        // then
        verify(policyDocumentRepository, times(1)).findById(POLICY_ID);
    }

    @Test
    void should_throw_policy_not_found_exception_when_policy_document_not_found_given() {
        // given
        ReflectionTestUtils.setField(policyNotificationMailProcessor, "policyId", POLICY_ID);
        PolicyNotificationTarget target = buildTarget(1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(buildUser(1L, "iu@coming.com")));
        given(policyDocumentRepository.findById(POLICY_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> policyNotificationMailProcessor.process(target))
                .isInstanceOf(PolicyNotFoundException.class);
    }
}
