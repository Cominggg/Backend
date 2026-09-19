package com.Coming.Backend.admin.dto;

import com.Coming.Backend.notice.entity.Notice;

import java.time.LocalDateTime;

public record AdminNoticeListItemResponse(
        Long id,
        String title,
        boolean active,
        LocalDateTime createdAt
) {
    public static AdminNoticeListItemResponse of(Notice notice) {
        return new AdminNoticeListItemResponse(
                notice.getId(),
                notice.getTitle(),
                notice.isActive(),
                notice.getCreatedAt()
        );
    }
}
