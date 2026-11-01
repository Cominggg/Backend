package com.Coming.Backend.auth.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.event.SuspensionNoticeEvent;
import com.Coming.Backend.report.entity.ReportReason;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
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
class SuspensionNoticeMailSenderTest {

    @InjectMocks
    private SuspensionNoticeMailSender suspensionNoticeMailSender;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private TemplateEngine templateEngine;

    private static final String TEMPLATE_NAME = "mail/suspension-notice";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(suspensionNoticeMailSender, "fromAddress", "coming@example.com");
        ReflectionTestUtils.setField(suspensionNoticeMailSender, "webBaseUrl", "https://www.comingg.com");
        given(javaMailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
        given(templateEngine.process(eq(TEMPLATE_NAME), any(Context.class))).willReturn("<html>notice</html>");
    }

    @Test
    void should_send_mail_with_period_reason_and_operation_policy_url_when_send_called() throws Exception {
        // given
        SuspensionNoticeEvent event = new SuspensionNoticeEvent(1L, "user@example.com",
                LocalDateTime.of(2026, 11, 5, 12, 0, 0), ReportReason.SPAM, "도배");

        // when
        suspensionNoticeMailSender.send(event);

        // then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq(TEMPLATE_NAME), contextCaptor.capture());
        Context context = contextCaptor.getValue();
        assertThat(context.getVariable("period")).isEqualTo("2026년 11월 5일 12:00까지");
        assertThat(context.getVariable("reasonLabel")).isEqualTo("스팸/광고");
        assertThat(context.getVariable("detail")).isEqualTo("도배");
        assertThat(context.getVariable("operationPolicyUrl")).isEqualTo("https://www.comingg.com/operation-policy");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getSubject()).isEqualTo("[커밍] 계정 이용 정지 안내");
        assertThat(messageCaptor.getValue().getAllRecipients()[0].toString()).isEqualTo("user@example.com");
    }

    @Test
    void should_show_permanent_label_when_suspended_until_is_null() {
        // given
        SuspensionNoticeEvent event = new SuspensionNoticeEvent(1L, "user@example.com", null, ReportReason.ILLEGAL, null);

        // when
        suspensionNoticeMailSender.send(event);

        // then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq(TEMPLATE_NAME), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getVariable("period")).isEqualTo("영구 정지");
        assertThat(contextCaptor.getValue().getVariable("reasonLabel")).isEqualTo("불법 정보");
    }
}
