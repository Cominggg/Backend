package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.PostCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @NotNull
        PostCategory category,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotNull
        Object content,

        List<@Valid EntityTagRequest> entityTags
) {
}
