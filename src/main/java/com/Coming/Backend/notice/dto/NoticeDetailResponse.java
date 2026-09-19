package com.Coming.Backend.notice.dto;

import java.time.LocalDateTime;

public record NoticeDetailResponse(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
