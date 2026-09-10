package com.Coming.Backend.post.repository;

import com.Coming.Backend.post.entity.PostRecommend;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRecommendRepository extends JpaRepository<PostRecommend, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<PostRecommend> findByUserIdAndPostId(Long userId, Long postId);
}
