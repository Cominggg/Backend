package com.Coming.Backend.post.repository;

import com.Coming.Backend.post.entity.Post;
import com.Coming.Backend.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE (:category IS NULL OR p.category = :category) ORDER BY p.createdAt DESC")
    Page<Post> findPosts(@Param("category") PostCategory category, Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.recommendCount = p.recommendCount + 1 WHERE p.id = :id")
    void incrementRecommendCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.recommendCount = p.recommendCount - 1 WHERE p.id = :id")
    void decrementRecommendCount(@Param("id") Long id);
}
