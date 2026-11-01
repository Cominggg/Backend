package com.Coming.Backend.auth.event;

import com.Coming.Backend.report.entity.ReportReason;
import java.time.LocalDateTime;

/**
 * @param suspendedUntil null이면 영구 정지
 */
public record SuspensionNoticeEvent(Long userId, String email, LocalDateTime suspendedUntil, ReportReason reason,
        String detail) {
}
