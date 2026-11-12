package com.Coming.Backend.post.repository;

import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.post.entity.PostEntityTag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PostEntityTagRepository extends JpaRepository<PostEntityTag, Long> {

    /**
     * 저장 순서(id)대로 반환한다. 태그는 본문 카드 등장 순서대로 저장되므로 응답 entityTags 순서가 본문 순서가 된다.
     */
    @Query("SELECT t FROM PostEntityTag t WHERE t.postId = :postId ORDER BY t.id")
    List<PostEntityTag> findByPostId(@Param("postId") Long postId);

    /**
     * 저장 순서(id)대로 반환한다. 게시글별로 묶어도 각 게시글 안의 순서가 본문 순서로 유지된다.
     */
    @Query("SELECT t FROM PostEntityTag t WHERE t.postId IN :postIds ORDER BY t.id")
    List<PostEntityTag> findByPostIdIn(@Param("postIds") Collection<Long> postIds);

    void deleteByPostId(Long postId);

    /**
     * 태그 자체가 아닌 게시글 작성일(post.createdAt) 기준으로 필터링한다.
     * 태그는 게시글 수정 시 삭제 후 재삽입되므로, 태그의 createdAt으로 필터링하면
     * 단순 수정만으로 최근 언급인 것처럼 집계될 수 있다.
     * 숨김 공연은 상위 K개를 자르기 전에 제외해야 결과 개수가 줄지 않는다.
     */
    @Query("""
            SELECT new com.Coming.Backend.post.repository.EntityTagCount(t.entityType, t.entityId, COUNT(t))
            FROM PostEntityTag t
            WHERE t.postId IN (SELECT p.id FROM Post p WHERE p.createdAt >= :since)
            AND NOT (t.entityType = com.Coming.Backend.post.entity.EntityType.CONCERT
                     AND t.entityId IN (SELECT c.id FROM Concert c WHERE c.status IN :hidden))
            GROUP BY t.entityType, t.entityId
            ORDER BY COUNT(t) DESC
            """)
    List<EntityTagCount> findTrendingEntityTags(@Param("since") LocalDateTime since,
                                                @Param("hidden") Collection<ConcertStatus> hidden,
                                                Pageable pageable);
}
