package com.Coming.Backend.post.repository;

import com.Coming.Backend.post.entity.Comment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.postId = :postId AND c.parentCommentId IS NULL ORDER BY c.createdAt ASC")
    Page<Comment> findTopLevelByPostId(@Param("postId") Long postId, Pageable pageable);

    List<Comment> findByParentCommentIdInOrderByCreatedAtAsc(Collection<Long> parentCommentIds);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id")
    void decrementLikeCount(@Param("id") Long id);
}
