package com.Coming.Backend.report.dto;

import com.Coming.Backend.report.entity.ReportReason;
import com.Coming.Backend.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;

public record ReportCreateRequest(
        @NotNull ReportTargetType targetType,
        @NotNull Long targetId,
        @NotNull ReportReason reason,
        String detail
) {
}
