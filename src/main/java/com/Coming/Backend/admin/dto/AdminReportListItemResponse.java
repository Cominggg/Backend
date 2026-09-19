package com.Coming.Backend.admin.dto;

import com.Coming.Backend.report.entity.Report;
import com.Coming.Backend.report.entity.ReportReason;
import com.Coming.Backend.report.entity.ReportStatus;
import com.Coming.Backend.report.entity.ReportTargetType;

import java.time.LocalDateTime;

public record AdminReportListItemResponse(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        ReportReason reason,
        ReportStatus status,
        Long reporterId,
        LocalDateTime createdAt
) {
    public static AdminReportListItemResponse of(Report report) {
        return new AdminReportListItemResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getStatus(),
                report.getReporterId(),
                report.getCreatedAt()
        );
    }
}
