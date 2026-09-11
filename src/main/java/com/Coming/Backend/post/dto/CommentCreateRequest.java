package com.Coming.Backend.post.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        @NotBlank
        String content,

        Long parentCommentId
) {
}
