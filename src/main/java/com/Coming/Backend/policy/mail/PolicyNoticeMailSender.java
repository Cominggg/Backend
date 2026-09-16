package com.Coming.Backend.policy.mail;

import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
public class PolicyNoticeMailSender {

    private static final String TEMPLATE_NAME = "mail/policy-change-notice";

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /**
     * 정책 변경 고지 메일을 발송한다.
     *
     * @throws IllegalStateException 메일 메시지 생성에 실패한 경우
     */
    public void send(String toEmail, PolicyDocument policyDocument) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject(buildSubject(policyDocument));
            helper.setText(renderHtml(policyDocument), true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("정책 변경 고지 메일 메시지 생성에 실패했습니다.", e);
        }
    }

    private String buildSubject(PolicyDocument policyDocument) {
        return "[Coming] " + labelOf(policyDocument.getType()) + " 변경 안내";
    }

    private String renderHtml(PolicyDocument policyDocument) {
        Context context = new Context();
        context.setVariable("policyTypeLabel", labelOf(policyDocument.getType()));
        context.setVariable("version", policyDocument.getVersion());
        context.setVariable("effectiveDate", policyDocument.getEffectiveDate());
        context.setVariable("changeSummary", policyDocument.getChangeSummary());
        context.setVariable("detailUrl", policyDocument.getDetailUrl());
        context.setVariable("requiresReconsent", policyDocument.isRequiresReconsent());
        context.setVariable("fromAddress", fromAddress);
        return templateEngine.process(TEMPLATE_NAME, context);
    }

    private String labelOf(PolicyType type) {
        return type == PolicyType.TERMS ? "이용약관" : "개인정보처리방침";
    }
}
