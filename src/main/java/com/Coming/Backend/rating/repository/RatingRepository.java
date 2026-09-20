package com.Coming.Backend.rating.repository;

import com.Coming.Backend.rating.entity.Rating;
import com.Coming.Backend.rating.entity.RatingTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndTargetTypeAndTargetId(Long userId, RatingTargetType targetType, Long targetId);

    @Query("SELECT r.targetId AS targetId, AVG(r.score) AS averageScore, COUNT(r) AS ratingCount " +
            "FROM Rating r WHERE r.targetType = :targetType AND r.targetId IN :targetIds GROUP BY r.targetId")
    List<RatingAggregate> aggregateByTargetIds(@Param("targetType") RatingTargetType targetType,
                                                @Param("targetIds") Collection<Long> targetIds);

    interface RatingAggregate {
        Long getTargetId();

        Double getAverageScore();

        Long getRatingCount();
    }
}
