package com.Coming.Backend.post.repository;

import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.entity.Post;
import com.Coming.Backend.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE (:category IS NULL OR p.category = :category) ORDER BY p.createdAt DESC")
    Page<Post> findPosts(@Param("category") PostCategory category, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.recommendCount DESC, p.createdAt DESC")
    List<Post> findPopularPosts(@Param("since") LocalDateTime since, Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.recommendCount = p.recommendCount + 1 WHERE p.id = :id")
    void incrementRecommendCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.recommendCount = p.recommendCount - 1 WHERE p.id = :id")
    void decrementRecommendCount(@Param("id") Long id);

    @Query("SELECT p.recommendCount FROM Post p WHERE p.id = :id")
    Long findRecommendCountById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :id")
    void incrementCommentCount(@Param("id") Long id);

    @Query("SELECT p FROM Post p WHERE p.id IN (SELECT t.postId FROM PostEntityTag t WHERE t.entityType = :entityType AND t.entityId = :entityId)")
    Page<Post> findByEntityTag(@Param("entityType") EntityType entityType, @Param("entityId") Long entityId, Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            WHERE LOWER(p.title) LIKE :q OR LOWER(p.contentText) LIKE :q
            OR p.id IN (
                SELECT t.postId FROM PostEntityTag t
                WHERE (t.entityType = com.Coming.Backend.post.entity.EntityType.ARTIST
                        AND t.entityId IN (SELECT a.id FROM Artist a WHERE LOWER(a.name) LIKE :q))
                   OR (t.entityType = com.Coming.Backend.post.entity.EntityType.CONCERT
                        AND t.entityId IN (SELECT c.id FROM Concert c WHERE LOWER(c.title) LIKE :q))
                   OR (t.entityType = com.Coming.Backend.post.entity.EntityType.RELEASE
                        AND t.entityId IN (SELECT r.id FROM ReleaseGroup r WHERE LOWER(r.title) LIKE :q))
            )
            ORDER BY p.createdAt DESC
            """)
    Page<Post> searchPosts(@Param("q") String q, Pageable pageable);
}
