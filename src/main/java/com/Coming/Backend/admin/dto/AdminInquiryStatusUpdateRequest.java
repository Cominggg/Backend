package com.Coming.Backend.admin.dto;

import com.Coming.Backend.inquiry.entity.InquiryStatus;
import jakarta.validation.constraints.NotNull;

public record AdminInquiryStatusUpdateRequest(
        @NotNull InquiryStatus status,
        String adminNote
) {
}
