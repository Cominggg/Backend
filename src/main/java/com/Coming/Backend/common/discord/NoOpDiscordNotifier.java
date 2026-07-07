package com.Coming.Backend.common.discord;

import com.Coming.Backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class NoOpDiscordNotifier implements DiscordNotifier {

    @Override
    public void notifyFiveXx(HttpServletRequest request, Exception e) {}

    @Override
    public void notifyFourXx(HttpServletRequest request, ErrorCode errorCode) {}
}
