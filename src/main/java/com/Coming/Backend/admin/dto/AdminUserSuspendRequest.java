package com.Coming.Backend.admin.dto;

import com.Coming.Backend.auth.dto.SuspensionCommand;
import com.Coming.Backend.auth.entity.SuspensionDuration;
import com.Coming.Backend.report.entity.ReportReason;
import jakarta.validation.constraints.NotNull;

/**
 * @param days   7, 30 또는 null(영구 정지)
 * @param detail 정지 통지 메일에 들어가는 상세 사유
 */
public record AdminUserSuspendRequest(
        Integer days,
        @NotNull ReportReason reason,
        String detail
) {
    public SuspensionCommand toCommand() {
        return new SuspensionCommand(SuspensionDuration.ofDays(days), reason, detail);
    }
}
