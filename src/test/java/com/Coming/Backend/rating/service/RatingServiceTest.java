package com.Coming.Backend.rating.service;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.rating.dto.RatingMeResponse;
import com.Coming.Backend.rating.dto.RatingSummary;
import com.Coming.Backend.rating.entity.Rating;
import com.Coming.Backend.rating.entity.RatingTargetType;
import com.Coming.Backend.rating.exception.InvalidRatingScoreException;
import com.Coming.Backend.rating.exception.RatingNotFoundException;
import com.Coming.Backend.rating.exception.RatingTargetNotFoundException;
import com.Coming.Backend.rating.exception.UnauthorizedException;
import com.Coming.Backend.rating.repository.RatingRepository;
import com.Coming.Backend.release.repository.ReleaseGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @InjectMocks
    private RatingService ratingService;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private ReleaseGroupRepository releaseGroupRepository;

    private static final Long USER_ID = 1L;
    private static final Long CONCERT_ID = 10L;

    // -------------------------------------------------------------------------
    // upsert
    // -------------------------------------------------------------------------

    @Test
    void should_throw_unauthorized_exception_when_user_id_is_null_on_upsert() {
        // when & then
        assertThatThrownBy(() -> ratingService.upsert(null, RatingTargetType.CONCERT, CONCERT_ID, BigDecimal.valueOf(4.5)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void should_throw_invalid_rating_score_exception_when_score_is_not_half_step() {
        // given
        BigDecimal invalidScore = BigDecimal.valueOf(4.3);

        // when & then
        assertThatThrownBy(() -> ratingService.upsert(USER_ID, RatingTargetType.CONCERT, CONCERT_ID, invalidScore))
                .isInstanceOf(InvalidRatingScoreException.class)
                .hasMessage(ErrorCode.INVALID_RATING_SCORE.getMessage());
    }

    @Test
    void should_throw_rating_target_not_found_exception_when_concert_does_not_exist() {
        // given
        given(concertRepository.existsById(CONCERT_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> ratingService.upsert(USER_ID, RatingTargetType.CONCERT, CONCERT_ID, BigDecimal.valueOf(4.5)))
                .isInstanceOf(RatingTargetNotFoundException.class)
                .hasMessage(ErrorCode.RATING_TARGET_NOT_FOUND.getMessage());
    }

    @Test
    void should_save_new_rating_when_no_existing_rating_found() {
        // given
        BigDecimal score = BigDecimal.valueOf(4.5);
        given(concertRepository.existsById(CONCERT_ID)).willReturn(true);
        given(ratingRepository.findByUserIdAndTargetTypeAndTargetId(USER_ID, RatingTargetType.CONCERT, CONCERT_ID))
                .willReturn(Optional.empty());

        // when
        ratingService.upsert(USER_ID, RatingTargetType.CONCERT, CONCERT_ID, score);

        // then
        verify(ratingRepository).save(any(Rating.class));
    }

    @Test
    void should_update_existing_rating_score_when_rating_already_exists() {
        // given
        Rating existingRating = Rating.builder()
                .userId(USER_ID)
                .targetType(RatingTargetType.CONCERT)
                .targetId(CONCERT_ID)
                .score(BigDecimal.valueOf(2.0))
                .build();
        given(concertRepository.existsById(CONCERT_ID)).willReturn(true);
        given(ratingRepository.findByUserIdAndTargetTypeAndTargetId(USER_ID, RatingTargetType.CONCERT, CONCERT_ID))
                .willReturn(Optional.of(existingRating));

        // when
        ratingService.upsert(USER_ID, RatingTargetType.CONCERT, CONCERT_ID, BigDecimal.valueOf(3.5));

        // then
        assertThat(existingRating.getScore()).isEqualByComparingTo(BigDecimal.valueOf(3.5));
        verify(ratingRepository, never()).save(any(Rating.class));
    }

    // -------------------------------------------------------------------------
    // getMine
    // -------------------------------------------------------------------------

    @Test
    void should_throw_unauthorized_exception_when_user_id_is_null_on_get_mine() {
        // when & then
        assertThatThrownBy(() -> ratingService.getMine(null, RatingTargetType.CONCERT, CONCERT_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void should_return_score_when_rating_exists() {
        // given
        Rating rating = Rating.builder()
                .userId(USER_ID)
                .targetType(RatingTargetType.CONCERT)
                .targetId(CONCERT_ID)
                .score(BigDecimal.valueOf(4.5))
                .build();
        given(ratingRepository.findByUserIdAndTargetTypeAndTargetId(USER_ID, RatingTargetType.CONCERT, CONCERT_ID))
                .willReturn(Optional.of(rating));

        // when
        RatingMeResponse response = ratingService.getMine(USER_ID, RatingTargetType.CONCERT, CONCERT_ID);

        // then
        assertThat(response.score()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    }

    @Test
    void should_return_null_score_when_rating_does_not_exist() {
        // given
        given(ratingRepository.findByUserIdAndTargetTypeAndTargetId(USER_ID, RatingTargetType.CONCERT, CONCERT_ID))
                .willReturn(Optional.empty());

        // when
        RatingMeResponse response = ratingService.getMine(USER_ID, RatingTargetType.CONCERT, CONCERT_ID);

        // then
        assertThat(response.score()).isNull();
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void should_throw_unauthorized_exception_when_user_id_is_null_on_delete() {
        // when & then
        assertThatThrownBy(() -> ratingService.delete(null, RatingTargetType.CONCERT, CONCERT_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void should_throw_rating_not_found_exception_when_rating_does_not_exist() {
        // given
        given(ratingRepository.findByUserIdAndTargetTypeAndTargetId(USER_ID, RatingTargetType.CONCERT, CONCERT_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> ratingService.delete(USER_ID, RatingTargetType.CONCERT, CONCERT_ID))
                .isInstanceOf(RatingNotFoundException.class)
                .hasMessage(ErrorCode.RATING_NOT_FOUND.getMessage());
    }

    @Test
    void should_delete_rating_when_rating_exists() {
        // given
        Rating rating = Rating.builder()
                .userId(USER_ID)
                .targetType(RatingTargetType.CONCERT)
                .targetId(CONCERT_ID)
                .score(BigDecimal.valueOf(4.5))
                .build();
        given(ratingRepository.findByUserIdAndTargetTypeAndTargetId(USER_ID, RatingTargetType.CONCERT, CONCERT_ID))
                .willReturn(Optional.of(rating));

        // when
        ratingService.delete(USER_ID, RatingTargetType.CONCERT, CONCERT_ID);

        // then
        verify(ratingRepository).delete(rating);
    }

    // -------------------------------------------------------------------------
    // getSummaries
    // -------------------------------------------------------------------------

    @Test
    void should_return_empty_map_when_target_ids_is_empty() {
        // when
        Map<Long, RatingSummary> summaries = ratingService.getSummaries(RatingTargetType.CONCERT, List.of());

        // then
        assertThat(summaries).isEmpty();
        verifyNoInteractions(ratingRepository);
    }

    @Test
    void should_return_summary_map_when_aggregates_exist() {
        // given
        RatingRepository.RatingAggregate aggregate1 = mock(RatingRepository.RatingAggregate.class);
        given(aggregate1.getTargetId()).willReturn(1L);
        given(aggregate1.getAverageScore()).willReturn(4.33);
        given(aggregate1.getRatingCount()).willReturn(3L);

        RatingRepository.RatingAggregate aggregate2 = mock(RatingRepository.RatingAggregate.class);
        given(aggregate2.getTargetId()).willReturn(2L);
        given(aggregate2.getAverageScore()).willReturn(5.0);
        given(aggregate2.getRatingCount()).willReturn(1L);

        given(ratingRepository.aggregateByTargetIds(any(RatingTargetType.class), anyCollection()))
                .willReturn(List.of(aggregate1, aggregate2));

        // when
        Map<Long, RatingSummary> summaries = ratingService.getSummaries(RatingTargetType.CONCERT, List.of(1L, 2L));

        // then
        assertThat(summaries.get(1L).averageRating()).isEqualTo(4.3);
        assertThat(summaries.get(1L).ratingCount()).isEqualTo(3L);
        assertThat(summaries.get(2L).averageRating()).isEqualTo(5.0);
        assertThat(summaries.get(2L).ratingCount()).isEqualTo(1L);
    }
}
