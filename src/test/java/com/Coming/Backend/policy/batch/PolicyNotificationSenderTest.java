package com.Coming.Backend.policy.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.entity.PolicyType;
import com.Coming.Backend.policy.mail.PolicyNoticeMailSender;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyNotificationSenderTest {

    @InjectMocks
    private PolicyNotificationSender policyNotificationSender;

    @Mock
    private PolicyNoticeMailSender policyNoticeMailSender;

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

    private PolicyNotificationTarget buildTarget() {
        return PolicyNotificationTarget.builder()
                .id(10L)
                .policyId(POLICY_ID)
                .userId(1L)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
    }

    @Test
    void should_mark_failed_when_user_is_null_given() {
        // given
        PolicyNotificationTarget target = buildTarget();
        PolicyDocument policyDocument = buildPolicyDocument();

        // when
        policyNotificationSender.sendAndMark(target, null, policyDocument);

        // then
        assertThat(target.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(policyNoticeMailSender, never()).send(any(), any());
    }

    @Test
    void should_mark_failed_when_user_has_no_email_given() {
        // given
        PolicyNotificationTarget target = buildTarget();
        User user = buildUser(1L, null);
        PolicyDocument policyDocument = buildPolicyDocument();

        // when
        policyNotificationSender.sendAndMark(target, user, policyDocument);

        // then
        assertThat(target.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(policyNoticeMailSender, never()).send(any(), any());
    }

    @Test
    void should_mark_sent_when_send_succeeds_given() {
        // given
        PolicyNotificationTarget target = buildTarget();
        User user = buildUser(1L, "iu@coming.com");
        PolicyDocument policyDocument = buildPolicyDocument();

        // when
        policyNotificationSender.sendAndMark(target, user, policyDocument);

        // then
        assertThat(target.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(target.getSentAt()).isNotNull();
        verify(policyNoticeMailSender).send("iu@coming.com", policyDocument);
    }

    @Test
    void should_mark_failed_and_increment_retry_count_when_send_throws_exception_given() {
        // given
        PolicyNotificationTarget target = buildTarget();
        User user = buildUser(1L, "iu@coming.com");
        PolicyDocument policyDocument = buildPolicyDocument();

        willThrow(new RuntimeException("smtp down")).given(policyNoticeMailSender).send("iu@coming.com", policyDocument);

        // when
        policyNotificationSender.sendAndMark(target, user, policyDocument);

        // then
        assertThat(target.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(target.getRetryCount()).isEqualTo(1);
    }
}
