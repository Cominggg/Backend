package com.Coming.Backend.post.repository;

import com.Coming.Backend.post.entity.PostEntityTag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PostEntityTagRepository extends JpaRepository<PostEntityTag, Long> {

    List<PostEntityTag> findByPostId(Long postId);

    List<PostEntityTag> findByPostIdIn(Collection<Long> postIds);

    void deleteByPostId(Long postId);

    /**
     * 태그 자체가 아닌 게시글 작성일(post.createdAt) 기준으로 필터링한다.
     * 태그는 게시글 수정 시 삭제 후 재삽입되므로, 태그의 createdAt으로 필터링하면
     * 단순 수정만으로 최근 언급인 것처럼 집계될 수 있다.
     */
    @Query("""
            SELECT new com.Coming.Backend.post.repository.EntityTagCount(t.entityType, t.entityId, COUNT(t))
            FROM PostEntityTag t
            WHERE t.postId IN (SELECT p.id FROM Post p WHERE p.createdAt >= :since)
            GROUP BY t.entityType, t.entityId
            ORDER BY COUNT(t) DESC
            """)
    List<EntityTagCount> findTrendingEntityTags(@Param("since") LocalDateTime since, Pageable pageable);
}
