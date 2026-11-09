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

    @Query("SELECT c FROM Comment c WHERE c.postId = :postId AND c.parentCommentId IS NULL ORDER BY c.createdAt ASC, c.id ASC")
    Page<Comment> findTopLevelByPostId(@Param("postId") Long postId, Pageable pageable);

    List<Comment> findByParentCommentIdInOrderByCreatedAtAscIdAsc(Collection<Long> parentCommentIds);

    @Query("SELECT c.id FROM Comment c WHERE c.postId = :postId")
    List<Long> findIdsByPostId(@Param("postId") Long postId);

    void deleteByPostId(Long postId);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id")
    void decrementLikeCount(@Param("id") Long id);

    /**
     * 아직 삭제되지 않은 댓글만 소프트 삭제한다. 동시 삭제 요청 중 한 요청만 1을 받는다.
     */
    @Modifying
    @Query("UPDATE Comment c SET c.deleted = true WHERE c.id = :id AND c.deleted = false")
    int softDeleteIfNotDeleted(@Param("id") Long id);

    @Query("SELECT c.likeCount FROM Comment c WHERE c.id = :id")
    Long findLikeCountById(@Param("id") Long id);
}
