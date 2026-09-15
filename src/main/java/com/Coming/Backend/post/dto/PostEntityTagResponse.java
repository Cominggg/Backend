package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.EntityType;

public record PostEntityTagResponse(
        EntityType entityType,
        Long entityId,
        String title,
        String subtitle,
        String thumbnailUrl,
        // TRACK 타입에서만 채워진다 (트랙이 속한 앨범 id). 그 외 타입은 항상 null.
        Long releaseGroupId
) {
    public static PostEntityTagResponse of(EntityType entityType, Long entityId, EntityCardResponse card) {
        return new PostEntityTagResponse(entityType, entityId, card.title(), card.subtitle(), card.thumbnailUrl(), card.releaseGroupId());
    }
}
