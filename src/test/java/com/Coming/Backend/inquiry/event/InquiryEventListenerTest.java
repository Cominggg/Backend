package com.Coming.Backend.inquiry.event;

import static org.mockito.Mockito.verify;

import com.Coming.Backend.common.discord.DiscordNotifier;
import com.Coming.Backend.inquiry.entity.Inquiry;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InquiryEventListenerTest {

    @InjectMocks
    private InquiryEventListener inquiryEventListener;

    @Mock
    private DiscordNotifier discordNotifier;

    @Test
    void should_call_discord_notifier_when_inquiry_created_event_received() {
        // given
        Inquiry inquiry = Inquiry.builder()
                .userId(1L)
                .type(InquiryType.CONCERT)
                .targetId(100L)
                .title("공연 정보 오류")
                .content("날짜가 잘못되어 있습니다.")
                .status(InquiryStatus.PENDING)
                .build();
        InquiryCreatedEvent event = new InquiryCreatedEvent(inquiry);

        // when
        inquiryEventListener.onInquiryCreated(event);

        // then
        verify(discordNotifier).notifyInquiry(inquiry);
    }
}
