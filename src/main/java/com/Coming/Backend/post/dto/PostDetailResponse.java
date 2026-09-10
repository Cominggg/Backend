package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.PostCategory;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long id,
        String authorNickname,
        PostCategory category,
        String title,
        Object content,
        List<PostEntityTagResponse> entityTags,
        Long recommendCount,
        Long viewCount,
        Boolean isRecommended,
        boolean isAuthor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
