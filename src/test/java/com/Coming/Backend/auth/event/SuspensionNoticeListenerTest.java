package com.Coming.Backend.auth.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.mail.SuspensionNoticeMailSender;
import com.Coming.Backend.report.entity.ReportReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuspensionNoticeListenerTest {

    @InjectMocks
    private SuspensionNoticeListener suspensionNoticeListener;

    @Mock
    private SuspensionNoticeMailSender suspensionNoticeMailSender;

    private final SuspensionNoticeEvent event =
            new SuspensionNoticeEvent(1L, "user@example.com", null, ReportReason.SPAM, null);

    @Test
    void should_send_mail_when_event_received() {
        // when
        suspensionNoticeListener.onSuspensionNotice(event);

        // then
        verify(suspensionNoticeMailSender).send(event);
    }

    @Test
    void should_swallow_exception_when_mail_sending_fails() {
        // given
        willThrow(new IllegalStateException("SMTP 오류")).given(suspensionNoticeMailSender).send(event);

        // when & then
        assertThatCode(() -> suspensionNoticeListener.onSuspensionNotice(event)).doesNotThrowAnyException();
    }
}
