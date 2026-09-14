package com.Coming.Backend.post.repository;

import com.Coming.Backend.post.entity.CommentLike;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    @Query("SELECT cl.commentId FROM CommentLike cl WHERE cl.userId = :userId AND cl.commentId IN :commentIds")
    List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("commentIds") Collection<Long> commentIds);

    void deleteByCommentIdIn(Collection<Long> commentIds);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.commentId IN (SELECT c.id FROM Comment c WHERE c.postId = :postId)")
    void deleteByCommentPostId(@Param("postId") Long postId);
}
