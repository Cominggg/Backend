package com.Coming.Backend.inquiry.dto;

import com.Coming.Backend.inquiry.entity.Inquiry;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;

public record InquiryListItemResponse(
        Long id,
        InquiryType type,
        String title,
        InquiryStatus status,
        String createdAt,
        String resultMessage,
        String rejectReason
) {
    public static InquiryListItemResponse from(Inquiry inquiry) {
        String adminNote = inquiry.getAdminNote();
        return new InquiryListItemResponse(
                inquiry.getId(),
                inquiry.getType(),
                inquiry.getTitle(),
                inquiry.getStatus(),
                inquiry.getCreatedAt().toLocalDate().toString(),
                inquiry.getStatus() == InquiryStatus.RESOLVED ? adminNote : null,
                inquiry.getStatus() == InquiryStatus.REJECTED ? adminNote : null
        );
    }
}
