package com.Coming.Backend.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank
        @Size(max = 500, message = "댓글은 500자를 초과할 수 없습니다")
        String content,

        Long parentCommentId
) {
}
