package com.Coming.Backend.admin.dto;

import com.Coming.Backend.common.util.TiptapContentCodec;
import com.Coming.Backend.notice.entity.Notice;

import java.time.LocalDateTime;

public record AdminNoticeDetailResponse(
        Long id,
        String title,
        Object content,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminNoticeDetailResponse of(Notice notice) {
        return new AdminNoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                TiptapContentCodec.read(notice.getContent()),
                notice.isActive(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
