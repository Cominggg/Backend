package com.Coming.Backend.report.event;

import static org.mockito.Mockito.verify;

import com.Coming.Backend.common.discord.DiscordNotifier;
import com.Coming.Backend.report.entity.Report;
import com.Coming.Backend.report.entity.ReportReason;
import com.Coming.Backend.report.entity.ReportStatus;
import com.Coming.Backend.report.entity.ReportTargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportEventListenerTest {

    @InjectMocks
    private ReportEventListener reportEventListener;

    @Mock
    private DiscordNotifier discordNotifier;

    @Test
    void should_call_discord_notifier_when_report_created_event_received() {
        // given
        Report report = Report.builder()
                .reporterId(1L)
                .targetType(ReportTargetType.POST)
                .targetId(100L)
                .reason(ReportReason.SPAM)
                .detail("도배성 게시글입니다.")
                .status(ReportStatus.PENDING)
                .build();
        ReportCreatedEvent event = new ReportCreatedEvent(report);

        // when
        reportEventListener.onReportCreated(event);

        // then
        verify(discordNotifier).notifyReport(report);
    }
}
