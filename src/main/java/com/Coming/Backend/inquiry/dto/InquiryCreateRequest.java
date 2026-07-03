package com.Coming.Backend.inquiry.dto;

import com.Coming.Backend.inquiry.entity.InquiryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InquiryCreateRequest(
        @NotNull
        InquiryType type,

        Long targetId,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        String content
) {
}
