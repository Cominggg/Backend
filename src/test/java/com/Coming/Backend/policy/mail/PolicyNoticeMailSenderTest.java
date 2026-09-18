package com.Coming.Backend.policy.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyType;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class PolicyNoticeMailSenderTest {

    @InjectMocks
    private PolicyNoticeMailSender policyNoticeMailSender;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private TemplateEngine templateEngine;

    private static final String FROM_ADDRESS = "coming@example.com";
    private static final String TO_EMAIL = "user@example.com";

    private PolicyDocument buildPolicyDocument(PolicyType type) {
        return PolicyDocument.builder()
                .id(1L)
                .type(type)
                .version("1.0.0")
                .effectiveDate(LocalDate.of(2026, 1, 1))
                .changeSummary("개인정보 수집 항목 변경")
                .detailUrl("https://coming.example.com/policy/1.0.0")
                .build();
    }

    @Test
    void should_send_mail_when_valid_policy_document_given() {
        // given
        ReflectionTestUtils.setField(policyNoticeMailSender, "fromAddress", FROM_ADDRESS);
        PolicyDocument policyDocument = buildPolicyDocument(PolicyType.TERMS);

        given(javaMailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
        given(templateEngine.process(eq("mail/policy-change-notice"), any(Context.class)))
                .willReturn("<html>notice</html>");

        // when
        policyNoticeMailSender.send(TO_EMAIL, policyDocument);

        // then
        verify(templateEngine).process(eq("mail/policy-change-notice"), any(Context.class));
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void should_pass_policy_document_fields_to_template_context_when_send_called() {
        // given
        ReflectionTestUtils.setField(policyNoticeMailSender, "fromAddress", FROM_ADDRESS);
        PolicyDocument policyDocument = buildPolicyDocument(PolicyType.PRIVACY);

        given(javaMailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
        given(templateEngine.process(eq("mail/policy-change-notice"), any(Context.class)))
                .willReturn("<html>notice</html>");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        // when
        policyNoticeMailSender.send(TO_EMAIL, policyDocument);

        // then
        verify(templateEngine).process(eq("mail/policy-change-notice"), contextCaptor.capture());
        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("changeSummary")).isEqualTo(policyDocument.getChangeSummary());
        assertThat(capturedContext.getVariable("effectiveDate")).isEqualTo(policyDocument.getEffectiveDate());
        assertThat(capturedContext.getVariable("detailUrl")).isEqualTo(policyDocument.getDetailUrl());
    }

    @Test
    void should_set_terms_label_when_policy_type_is_terms() {
        // given
        ReflectionTestUtils.setField(policyNoticeMailSender, "fromAddress", FROM_ADDRESS);
        PolicyDocument policyDocument = buildPolicyDocument(PolicyType.TERMS);

        given(javaMailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
        given(templateEngine.process(eq("mail/policy-change-notice"), any(Context.class)))
                .willReturn("<html>notice</html>");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        // when
        policyNoticeMailSender.send(TO_EMAIL, policyDocument);

        // then
        verify(templateEngine).process(eq("mail/policy-change-notice"), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getVariable("policyTypeLabel")).isEqualTo("이용약관");
    }

    @Test
    void should_set_privacy_label_when_policy_type_is_privacy() {
        // given
        ReflectionTestUtils.setField(policyNoticeMailSender, "fromAddress", FROM_ADDRESS);
        PolicyDocument policyDocument = buildPolicyDocument(PolicyType.PRIVACY);

        given(javaMailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
        given(templateEngine.process(eq("mail/policy-change-notice"), any(Context.class)))
                .willReturn("<html>notice</html>");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        // when
        policyNoticeMailSender.send(TO_EMAIL, policyDocument);

        // then
        verify(templateEngine).process(eq("mail/policy-change-notice"), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getVariable("policyTypeLabel")).isEqualTo("개인정보처리방침");
    }
}
