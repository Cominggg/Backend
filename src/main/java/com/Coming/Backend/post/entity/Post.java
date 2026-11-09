package com.Coming.Backend.post.entity;

import com.Coming.Backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private PostCategory category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @Column(name = "content_text", nullable = false, columnDefinition = "text")
    private String contentText;

    @Column(name = "recommend_count", nullable = false)
    private Long recommendCount;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount;

    // 공연 태그 중 대표 공연 id. 공연 행을 참조하지만 태그와 같이 FK 없이 둔다.
    @Column(name = "cover_concert_id")
    private Long coverConcertId;

    public boolean isAuthoredBy(Long userId) {
        return userId != null && this.userId.equals(userId);
    }

    public void update(PostCategory category, String title, String content, String contentText) {
        if (category != null) this.category = category;
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (contentText != null) this.contentText = contentText;
    }

    /**
     * 대표 공연을 바꾼다. update와 달리 null이면 대표 공연을 비운다.
     */
    public void changeCoverConcert(Long coverConcertId) {
        this.coverConcertId = coverConcertId;
    }

    /**
     * 수정 요청의 대표 공연을 반영한다. 요청 값이 없으면 기존 대표를 유지하되, 태그 교체로 태그에서 빠졌으면 비운다.
     * requestedConcertIds가 null이면 태그를 바꾸지 않는 요청이다.
     */
    public void applyCoverConcert(Set<Long> requestedConcertIds, Long requestedCoverConcertId) {
        if (requestedCoverConcertId != null) {
            this.coverConcertId = requestedCoverConcertId;
        } else if (requestedConcertIds != null && this.coverConcertId != null
                && !requestedConcertIds.contains(this.coverConcertId)) {
            this.coverConcertId = null;
        }
    }
}
