package com.Coming.Backend.notice.dto;

import java.time.LocalDateTime;

public record NoticeSummaryResponse(
        Long id,
        String title,
        LocalDateTime createdAt
) {
}
