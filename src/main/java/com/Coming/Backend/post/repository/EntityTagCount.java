package com.Coming.Backend.post.repository;

import com.Coming.Backend.post.entity.EntityType;

public record EntityTagCount(EntityType entityType, Long entityId, Long count) {
}
