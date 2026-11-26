package com.Coming.Backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminNoticeCreateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull Object content,
        Boolean active
) {
}
