package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.EntityType;

public record EntityCardResponse(
        EntityType type,
        Long id,
        String title,
        String subtitle,
        String thumbnailUrl,
        // TRACK 타입에서만 채워진다 (트랙이 속한 앨범 id). 그 외 타입은 항상 null.
        Long releaseGroupId
) {
}
