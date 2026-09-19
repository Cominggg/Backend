package com.Coming.Backend.admin.dto;

public record AdminNoticeUpdateRequest(
        String title,
        String content,
        Boolean active
) {
}
