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

        @Size(max = 10, message = "태그는 10개를 초과할 수 없습니다")
        List<@Valid EntityTagRequest> entityTags
) {
}
