package com.Coming.Backend.admin.dto;

import com.Coming.Backend.inquiry.entity.Inquiry;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;

public record AdminInquiryDetailResponse(
        Long id,
        InquiryType type,
        String title,
        InquiryStatus status,
        String createdAt,
        String resultMessage,
        String rejectReason,
        Long userId,
        String userNickname,
        String content,
        Long targetId
) {
    public static AdminInquiryDetailResponse of(Inquiry inquiry, String userNickname) {
        String adminNote = inquiry.getAdminNote();
        return new AdminInquiryDetailResponse(
                inquiry.getId(),
                inquiry.getType(),
                inquiry.getTitle(),
                inquiry.getStatus(),
                inquiry.getCreatedAt().toLocalDate().toString(),
                inquiry.getStatus() == InquiryStatus.RESOLVED ? adminNote : null,
                inquiry.getStatus() == InquiryStatus.REJECTED ? adminNote : null,
                inquiry.getUserId(),
                userNickname,
                inquiry.getContent(),
                inquiry.getTargetId()
        );
    }
}
