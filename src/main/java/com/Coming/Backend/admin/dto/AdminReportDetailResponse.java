package com.Coming.Backend.admin.dto;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.report.entity.Report;
import com.Coming.Backend.report.entity.ReportReason;
import com.Coming.Backend.report.entity.ReportStatus;
import com.Coming.Backend.report.entity.ReportTargetType;

import com.fasterxml.jackson.annotation.JsonFormat;
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
        String reporterNickname,
        LocalDateTime createdAt,
        Long targetAuthorId,
        String targetAuthorNickname,
        boolean targetAuthorSuspended,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime targetAuthorSuspendedUntil
) {
    /**
     * @param targetAuthor 신고 대상 작성자. 대상이 삭제돼 작성자를 알 수 없으면 null
     */
    public static AdminReportDetailResponse of(Report report, String reporterNickname, User targetAuthor) {
        boolean isSuspended = targetAuthor != null && targetAuthor.getStatus() == UserStatus.SUSPENDED;
        return new AdminReportDetailResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getAdminNote(),
                report.getReporterId(),
                reporterNickname,
                report.getCreatedAt(),
                targetAuthor == null ? null : targetAuthor.getId(),
                targetAuthor == null ? null : targetAuthor.getNickname(),
                isSuspended,
                isSuspended ? targetAuthor.getSuspendedUntil() : null
        );
    }
}
