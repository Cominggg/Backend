package com.Coming.Backend.auth.dto;

import com.Coming.Backend.auth.entity.SuspensionDuration;
import com.Coming.Backend.report.entity.ReportReason;

/**
 * @param detail 회원에게 통지 메일로 전달되는 상세 사유 (nullable)
 */
public record SuspensionCommand(SuspensionDuration duration, ReportReason reason, String detail) {
}
