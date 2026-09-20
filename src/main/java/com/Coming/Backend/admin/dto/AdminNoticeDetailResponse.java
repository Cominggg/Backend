package com.Coming.Backend.admin.dto;

import com.Coming.Backend.notice.entity.Notice;

import java.time.LocalDateTime;

public record AdminNoticeDetailResponse(
        Long id,
        String title,
        String content,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminNoticeDetailResponse of(Notice notice) {
        return new AdminNoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.isActive(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
