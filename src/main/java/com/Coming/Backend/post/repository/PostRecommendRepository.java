package com.Coming.Backend.post.repository;

import com.Coming.Backend.post.entity.PostRecommend;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRecommendRepository extends JpaRepository<PostRecommend, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    long deleteByUserIdAndPostId(Long userId, Long postId);

    void deleteByPostId(Long postId);
}
