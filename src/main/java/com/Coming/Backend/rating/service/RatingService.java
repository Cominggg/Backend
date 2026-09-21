package com.Coming.Backend.rating.service;

import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.exception.ConcertNotEndedException;
import com.Coming.Backend.concert.exception.UnauthorizedException;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.rating.dto.RatingMeResponse;
import com.Coming.Backend.rating.dto.RatingSummary;
import com.Coming.Backend.rating.entity.Rating;
import com.Coming.Backend.rating.entity.RatingTargetType;
import com.Coming.Backend.rating.exception.InvalidRatingScoreException;
import com.Coming.Backend.rating.exception.RatingNotFoundException;
import com.Coming.Backend.rating.exception.RatingTargetNotFoundException;
import com.Coming.Backend.rating.repository.RatingRepository;
import com.Coming.Backend.release.repository.ReleaseGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingService {

    private static final BigDecimal MIN_SCORE = BigDecimal.valueOf(0.5);
    private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(5.0);
    private static final BigDecimal SCORE_STEP = BigDecimal.valueOf(0.5);

    private final RatingRepository ratingRepository;
    private final ConcertRepository concertRepository;
    private final ReleaseGroupRepository releaseGroupRepository;

    /**
     * 별점을 등록하거나 수정한다. 대상이 존재하지 않으면 RatingTargetNotFoundException,
     * score가 0.5~5.0 범위의 0.5 단위가 아니면 InvalidRatingScoreException,
     * 공연이 ENDED 상태가 아니면 ConcertNotEndedException을 던진다.
     *
     * <p>등록·수정은 DB의 유니크 제약을 이용한 원자적 upsert로 처리되어, 동일 사용자가 같은 대상에
     * 동시에 첫 별점을 등록해도 유니크 제약 위반 없이 안전하게 처리된다.</p>
     */
    @Transactional
    public void upsert(Long userId, RatingTargetType targetType, Long targetId, BigDecimal score) {
        if (userId == null) {
            throw new UnauthorizedException();
        }
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0
                || score.remainder(SCORE_STEP).compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidRatingScoreException();
        }
        validateTarget(targetType, targetId);
        ratingRepository.upsert(userId, targetType.name(), targetId, score);
    }

    /**
     * 내 별점을 조회한다. 등록한 적이 없으면 score가 null인 응답을 반환한다.
     */
    public RatingMeResponse getMine(Long userId, RatingTargetType targetType, Long targetId) {
        if (userId == null) {
            throw new UnauthorizedException();
        }
        return ratingRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .map(rating -> new RatingMeResponse(rating.getScore()))
                .orElseGet(RatingMeResponse::empty);
    }

    /**
     * 별점을 취소한다. 등록된 별점이 없으면 RatingNotFoundException을 던진다.
     */
    @Transactional
    public void delete(Long userId, RatingTargetType targetType, Long targetId) {
        if (userId == null) {
            throw new UnauthorizedException();
        }
        Rating rating = ratingRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .orElseThrow(RatingNotFoundException::new);
        ratingRepository.delete(rating);
    }

    /**
     * 대상 ID 목록에 대한 평균 별점·평가 개수를 조회한다. 별점이 없는 대상은 결과 맵에 포함되지 않는다.
     */
    public Map<Long, RatingSummary> getSummaries(RatingTargetType targetType,
                                                  Collection<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Map.of();
        }
        return ratingRepository.aggregateByTargetIds(targetType, targetIds).stream()
                .collect(Collectors.toMap(
                        RatingRepository.RatingAggregate::getTargetId,
                        aggregate -> new RatingSummary(
                                Math.round(aggregate.getAverageScore() * 10) / 10.0,
                                aggregate.getRatingCount())
                ));
    }

    private void validateTarget(RatingTargetType targetType, Long targetId) {
        switch (targetType) {
            case CONCERT -> {
                Concert concert = concertRepository.findById(targetId)
                        .orElseThrow(RatingTargetNotFoundException::new);
                if (concert.getStatus() != ConcertStatus.ENDED) {
                    throw new ConcertNotEndedException();
                }
            }
            case RELEASE -> {
                if (!releaseGroupRepository.existsById(targetId)) {
                    throw new RatingTargetNotFoundException();
                }
            }
        }
    }
}
