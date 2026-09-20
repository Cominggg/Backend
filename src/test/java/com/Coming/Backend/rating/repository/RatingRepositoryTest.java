package com.Coming.Backend.rating.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.rating.entity.Rating;
import com.Coming.Backend.rating.entity.RatingTargetType;
import com.Coming.Backend.rating.repository.RatingRepository.RatingAggregate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RatingRepositoryTest {

    @Autowired
    private RatingRepository ratingRepository;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long TARGET_ID = 10L;
    private static final Long OTHER_TARGET_ID = 20L;
    private static final Long UNRATED_TARGET_ID = 30L;

    private Rating buildRating(Long userId, RatingTargetType targetType, Long targetId, BigDecimal score) {
        return Rating.builder()
                .userId(userId)
                .targetType(targetType)
                .targetId(targetId)
                .score(score)
                .build();
    }

    @Test
    void should_return_rating_when_matching_combination_exists() {
        // given
        ratingRepository.save(buildRating(USER_ID, RatingTargetType.CONCERT, TARGET_ID, BigDecimal.valueOf(4)));

        // when
        Optional<Rating> found = ratingRepository.findByUserIdAndTargetTypeAndTargetId(
                USER_ID, RatingTargetType.CONCERT, TARGET_ID);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getScore()).isEqualByComparingTo(BigDecimal.valueOf(4));
    }

    @Test
    void should_return_empty_when_user_id_differs() {
        // given
        ratingRepository.save(buildRating(USER_ID, RatingTargetType.CONCERT, TARGET_ID, BigDecimal.valueOf(4)));

        // when
        Optional<Rating> found = ratingRepository.findByUserIdAndTargetTypeAndTargetId(
                OTHER_USER_ID, RatingTargetType.CONCERT, TARGET_ID);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void should_return_empty_when_target_type_differs() {
        // given
        ratingRepository.save(buildRating(USER_ID, RatingTargetType.CONCERT, TARGET_ID, BigDecimal.valueOf(4)));

        // when
        Optional<Rating> found = ratingRepository.findByUserIdAndTargetTypeAndTargetId(
                USER_ID, RatingTargetType.RELEASE, TARGET_ID);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void should_return_empty_when_target_id_differs() {
        // given
        ratingRepository.save(buildRating(USER_ID, RatingTargetType.CONCERT, TARGET_ID, BigDecimal.valueOf(4)));

        // when
        Optional<Rating> found = ratingRepository.findByUserIdAndTargetTypeAndTargetId(
                USER_ID, RatingTargetType.CONCERT, OTHER_TARGET_ID);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void should_aggregate_average_and_count_when_multiple_users_rate_same_target() {
        // given
        ratingRepository.save(buildRating(USER_ID, RatingTargetType.CONCERT, TARGET_ID, BigDecimal.valueOf(4)));
        ratingRepository.save(buildRating(OTHER_USER_ID, RatingTargetType.CONCERT, TARGET_ID, BigDecimal.valueOf(5)));

        // when
        List<RatingAggregate> aggregates = ratingRepository.aggregateByTargetIds(
                RatingTargetType.CONCERT, List.of(TARGET_ID));

        // then
        assertThat(aggregates).hasSize(1);
        assertThat(aggregates.get(0).getTargetId()).isEqualTo(TARGET_ID);
        assertThat(aggregates.get(0).getAverageScore()).isEqualTo(4.5);
        assertThat(aggregates.get(0).getRatingCount()).isEqualTo(2L);
    }

    @Test
    void should_aggregate_independently_when_multiple_target_ids_given() {
        // given
        ratingRepository.save(buildRating(USER_ID, RatingTargetType.CONCERT, TARGET_ID, BigDecimal.valueOf(4)));
        ratingRepository.save(buildRating(USER_ID, RatingTargetType.CONCERT, OTHER_TARGET_ID, BigDecimal.valueOf(2)));

        // when
        List<RatingAggregate> aggregates = ratingRepository.aggregateByTargetIds(
                RatingTargetType.CONCERT, List.of(TARGET_ID, OTHER_TARGET_ID));

        // then
        assertThat(aggregates).hasSize(2);
        assertThat(aggregates)
                .filteredOn(aggregate -> aggregate.getTargetId().equals(TARGET_ID))
                .extracting(RatingAggregate::getAverageScore)
                .containsExactly(4.0);
        assertThat(aggregates)
                .filteredOn(aggregate -> aggregate.getTargetId().equals(OTHER_TARGET_ID))
                .extracting(RatingAggregate::getAverageScore)
                .containsExactly(2.0);
    }

    @Test
    void should_exclude_rating_from_aggregate_when_target_type_differs() {
        // given
        ratingRepository.save(buildRating(USER_ID, RatingTargetType.RELEASE, TARGET_ID, BigDecimal.valueOf(5)));

        // when
        List<RatingAggregate> aggregates = ratingRepository.aggregateByTargetIds(
                RatingTargetType.CONCERT, List.of(TARGET_ID));

        // then
        assertThat(aggregates).isEmpty();
    }

    @Test
    void should_exclude_target_id_from_result_when_no_ratings_exist() {
        // given
        ratingRepository.save(buildRating(USER_ID, RatingTargetType.CONCERT, TARGET_ID, BigDecimal.valueOf(4)));

        // when
        List<RatingAggregate> aggregates = ratingRepository.aggregateByTargetIds(
                RatingTargetType.CONCERT, List.of(TARGET_ID, UNRATED_TARGET_ID));

        // then
        assertThat(aggregates)
                .extracting(RatingAggregate::getTargetId)
                .containsExactly(TARGET_ID);
    }
}
