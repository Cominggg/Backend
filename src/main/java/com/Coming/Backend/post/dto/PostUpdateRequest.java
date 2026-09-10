package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.PostCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostUpdateRequest(
        PostCategory category,

        @Size(max = 255)
        String title,

        Object content,

        List<@Valid EntityTagRequest> entityTags
) {
}
