package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.EntityType;

public record EntityCardResponse(
        EntityType type,
        Long id,
        String title,
        String subtitle,
        String thumbnailUrl,
        Long releaseGroupId
) {
}
