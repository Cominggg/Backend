package com.Coming.Backend.admin.dto;

import com.Coming.Backend.report.entity.Report;
import com.Coming.Backend.report.entity.ReportReason;
import com.Coming.Backend.report.entity.ReportStatus;
import com.Coming.Backend.report.entity.ReportTargetType;

import java.time.LocalDateTime;

public record AdminReportDetailResponse(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        ReportReason reason,
        String detail,
        ReportStatus status,
        String adminNote,
        Long reporterId,
        LocalDateTime createdAt
) {
    public static AdminReportDetailResponse of(Report report) {
        return new AdminReportDetailResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getAdminNote(),
                report.getReporterId(),
                report.getCreatedAt()
        );
    }
}
