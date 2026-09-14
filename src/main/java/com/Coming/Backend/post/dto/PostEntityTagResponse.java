package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.EntityType;

public record PostEntityTagResponse(
        EntityType entityType,
        Long entityId,
        String title,
        String subtitle,
        String thumbnailUrl
) {
    public static PostEntityTagResponse of(EntityType entityType, Long entityId, EntityCardResponse card) {
        return new PostEntityTagResponse(entityType, entityId, card.title(), card.subtitle(), card.thumbnailUrl());
    }
}
