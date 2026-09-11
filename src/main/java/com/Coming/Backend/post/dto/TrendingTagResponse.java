package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.EntityType;

public record TrendingTagResponse(
        EntityType entityType,
        Long entityId,
        String title,
        Long count
) {
}
