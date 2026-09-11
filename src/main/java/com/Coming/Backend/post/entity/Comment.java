package com.Coming.Backend.post.entity;

import com.Coming.Backend.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Comment extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    public boolean isAuthoredBy(Long userId) {
        return userId != null && this.userId.equals(userId);
    }

    public boolean isReply() {
        return parentCommentId != null;
    }

    public void softDelete() {
        this.deleted = true;
    }
}
