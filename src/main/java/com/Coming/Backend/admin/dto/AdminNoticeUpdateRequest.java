package com.Coming.Backend.admin.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminNoticeUpdateRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "제목은 공백일 수 없습니다")
        @Size(max = 255)
        String title,

        @Pattern(regexp = "(?s).*\\S.*", message = "내용은 공백일 수 없습니다")
        String content,

        Boolean active
) {
}
