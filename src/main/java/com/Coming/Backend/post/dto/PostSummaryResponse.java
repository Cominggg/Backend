package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.PostCategory;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryResponse(
        Long id,
        String authorNickname,
        PostCategory category,
        String title,
        List<PostEntityTagResponse> entityTags,
        Long recommendCount,
        Long viewCount,
        LocalDateTime createdAt
) {
}
