package com.Coming.Backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminNoticeCreateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        Boolean active
) {
}
