package com.Coming.Backend.common.discord;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.inquiry.entity.Inquiry;
import jakarta.servlet.http.HttpServletRequest;

public interface DiscordNotifier {

    void notifyFiveXx(HttpServletRequest request, Exception e);

    void notifyFourXx(HttpServletRequest request, ErrorCode errorCode);

    void notifyInquiry(Inquiry inquiry);
}
