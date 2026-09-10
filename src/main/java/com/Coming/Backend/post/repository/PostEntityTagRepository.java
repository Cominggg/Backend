package com.Coming.Backend.post.repository;

import com.Coming.Backend.post.entity.PostEntityTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PostEntityTagRepository extends JpaRepository<PostEntityTag, Long> {

    List<PostEntityTag> findByPostId(Long postId);

    List<PostEntityTag> findByPostIdIn(Collection<Long> postIds);

    void deleteByPostId(Long postId);
}
