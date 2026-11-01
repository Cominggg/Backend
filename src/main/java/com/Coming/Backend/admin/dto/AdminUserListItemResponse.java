package com.Coming.Backend.admin.dto;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.report.entity.ReportReason;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * @param suspendedUntil 정지(SUSPENDED) 상태에서 null이면 영구 정지
 */
public record AdminUserListItemResponse(
        Long id,
        String email,
        String nickname,
        String provider,
        UserRole role,
        UserStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime suspendedUntil,
        ReportReason suspensionReason,
        String createdAt
) {
    public static AdminUserListItemResponse from(User user) {
        return new AdminUserListItemResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProvider(),
                user.getRole(),
                user.getStatus(),
                user.getSuspendedUntil(),
                user.getSuspensionReason(),
                user.getCreatedAt().toLocalDate().toString()
        );
    }
}
