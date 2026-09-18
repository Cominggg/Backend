package com.Coming.Backend.post.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String authorNickname,
        boolean isAuthor,
        String content,
        Long likeCount,
        Boolean isLiked,
        LocalDateTime createdAt,
        List<CommentResponse> replies
) {
}
