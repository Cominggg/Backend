package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotNull
        PostCategory category,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotNull
        Object content,

        // 본문 언급 카드 중 공연 하나의 id. 수정에서 null이면 기존 대표를 유지한다(content 수정으로 카드가 빠지면 비운다)
        Long coverConcertId
) {
}
