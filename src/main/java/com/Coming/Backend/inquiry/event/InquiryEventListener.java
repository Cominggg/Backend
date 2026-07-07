package com.Coming.Backend.inquiry.event;

import com.Coming.Backend.common.discord.DiscordNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InquiryEventListener {

    private final DiscordNotifier discordNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInquiryCreated(InquiryCreatedEvent event) {
        discordNotifier.notifyInquiry(event.inquiry());
    }
}
