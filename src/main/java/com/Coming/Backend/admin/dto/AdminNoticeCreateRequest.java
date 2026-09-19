package com.Coming.Backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminNoticeCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        Boolean active
) {
}
