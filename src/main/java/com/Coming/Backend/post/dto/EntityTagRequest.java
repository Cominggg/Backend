package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.EntityType;
import jakarta.validation.constraints.NotNull;

public record EntityTagRequest(
        @NotNull
        EntityType entityType,

        @NotNull
        Long entityId
) {
}
