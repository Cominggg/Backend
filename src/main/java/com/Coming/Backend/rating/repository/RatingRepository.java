package com.Coming.Backend.rating.repository;

import com.Coming.Backend.rating.entity.Rating;
import com.Coming.Backend.rating.entity.RatingTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndTargetTypeAndTargetId(
            Long userId, RatingTargetType targetType, Long targetId);

    /**
     * user_id·target_type·target_id 유니크 제약을 이용해 별점을 원자적으로 등록·수정한다.
     * 동시에 같은 대상에 첫 별점을 등록하는 요청이 몰려도 유니크 제약 위반 없이 하나는 삽입, 나머지는 갱신으로 처리된다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO rating (user_id, target_type, target_id, score, created_at, updated_at)
            VALUES (:userId, :targetType, :targetId, :score, now(), now())
            ON CONFLICT (user_id, target_type, target_id)
            DO UPDATE SET score = :score, updated_at = now()
            """, nativeQuery = true)
    void upsert(@Param("userId") Long userId, @Param("targetType") String targetType,
                @Param("targetId") Long targetId, @Param("score") BigDecimal score);

    @Query("SELECT r.targetId AS targetId, AVG(r.score) AS averageScore, COUNT(r) AS ratingCount " +
            "FROM Rating r WHERE r.targetType = :targetType " +
            "AND r.targetId IN :targetIds GROUP BY r.targetId")
    List<RatingAggregate> aggregateByTargetIds(@Param("targetType") RatingTargetType targetType,
                                                @Param("targetIds") Collection<Long> targetIds);

    interface RatingAggregate {
        Long getTargetId();

        Double getAverageScore();

        Long getRatingCount();
    }
}
