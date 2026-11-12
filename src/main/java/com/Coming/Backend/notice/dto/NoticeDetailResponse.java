package com.Coming.Backend.notice.dto;

import java.time.LocalDateTime;

public record NoticeDetailResponse(
        Long id,
        String title,
        Object content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
