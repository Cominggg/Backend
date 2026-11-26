package com.Coming.Backend.post.dto;

import com.Coming.Backend.post.entity.PostCategory;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long id,
        String authorNickname,
        PostCategory category,
        String title,
        Object content,
        List<PostEntityTagResponse> entityTags,
        // 대표 공연 id. 응답 entityTags에 없는 공연(숨김·삭제)이면 null
        Long coverConcertId,
        Long recommendCount,
        Long viewCount,
        Long commentCount,
        Boolean isRecommended,
        boolean isAuthor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
