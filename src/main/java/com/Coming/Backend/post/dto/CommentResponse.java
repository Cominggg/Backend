package com.Coming.Backend.post.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String authorNickname,
        boolean isAuthor,
        // 댓글 작성자가 게시글 작성자와 같은 회원인지(현재 사용자와 무관). 삭제된 댓글은 false
        boolean isPostAuthor,
        String content,
        boolean isDeleted,
        Long likeCount,
        Boolean isLiked,
        LocalDateTime createdAt,
        List<CommentResponse> replies
) {
}
