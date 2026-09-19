package com.Coming.Backend.admin.dto;

import com.Coming.Backend.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record AdminReportStatusUpdateRequest(
        @NotNull ReportStatus status,
        String adminNote,
        Boolean deleteTarget
) {
}
