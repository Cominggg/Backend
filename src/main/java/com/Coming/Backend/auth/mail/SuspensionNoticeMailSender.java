package com.Coming.Backend.auth.mail;

import com.Coming.Backend.auth.event.SuspensionNoticeEvent;
import com.Coming.Backend.report.entity.ReportReason;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
public class SuspensionNoticeMailSender {

    private static final String TEMPLATE_NAME = "mail/suspension-notice";
    private static final String SUBJECT = "[커밍] 계정 이용 정지 안내";
    private static final String FROM_NAME = "커밍";
    private static final String LOGO_CONTENT_ID = "coming-logo";
    private static final String LOGO_PATH = "mail-assets/logo.png";
    private static final String OPERATION_POLICY_PATH = "/operation-policy";
    private static final DateTimeFormatter UNTIL_FORMAT = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm");
    private static final String PERMANENT_LABEL = "영구 정지";
    private static final Map<ReportReason, String> REASON_LABELS = Map.of(
            ReportReason.SPAM, "스팸/광고",
            ReportReason.ABUSE, "욕설/비방",
            ReportReason.SEXUAL, "음란물",
            ReportReason.ILLEGAL, "불법 정보",
            ReportReason.COPYRIGHT, "저작권 침해",
            ReportReason.PRIVACY, "개인정보 노출",
            ReportReason.ETC, "기타"
    );

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.web-base-url}")
    private String webBaseUrl;

    /**
     * 정지 기간·사유·이의신청 방법을 담은 정지 통지 메일을 발송한다.
     *
     * @throws IllegalStateException 메일 메시지 생성에 실패한 경우
     */
    public void send(SuspensionNoticeEvent event) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(event.email());
            helper.setFrom(fromAddress, FROM_NAME);
            helper.setSubject(SUBJECT);
            helper.setText(renderHtml(event), true);
            helper.addInline(LOGO_CONTENT_ID, new ClassPathResource(LOGO_PATH));
            javaMailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("정지 통지 메일 메시지 생성에 실패했습니다.", e);
        }
    }

    private String renderHtml(SuspensionNoticeEvent event) {
        Context context = new Context();
        context.setVariable("period", event.suspendedUntil() == null
                ? PERMANENT_LABEL
                : event.suspendedUntil().format(UNTIL_FORMAT) + "까지");
        context.setVariable("reasonLabel", REASON_LABELS.get(event.reason()));
        context.setVariable("detail", event.detail());
        context.setVariable("operationPolicyUrl", webBaseUrl + OPERATION_POLICY_PATH);
        context.setVariable("fromAddress", fromAddress);
        context.setVariable("logoContentId", LOGO_CONTENT_ID);
        return templateEngine.process(TEMPLATE_NAME, context);
    }
}
