package com.Coming.Backend.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Coming.Backend.common.exception.InvalidInputException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SuspensionDurationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 15, 30, 45, 987_000_000);

    @Test
    void should_return_seven_days_later_truncated_to_seconds_when_days_7() {
        // when
        LocalDateTime until = SuspensionDuration.DAYS_7.untilFrom(NOW);

        // then
        assertThat(until).isEqualTo(LocalDateTime.of(2026, 11, 5, 15, 30, 45));
    }

    @Test
    void should_return_thirty_days_later_when_days_30() {
        // when
        LocalDateTime until = SuspensionDuration.DAYS_30.untilFrom(NOW);

        // then
        assertThat(until).isEqualTo(LocalDateTime.of(2026, 11, 28, 15, 30, 45));
    }

    @Test
    void should_return_null_when_permanent() {
        // when & then
        assertThat(SuspensionDuration.PERMANENT.untilFrom(NOW)).isNull();
    }

    @Test
    void should_map_days_to_duration_when_of_days() {
        // when & then
        assertThat(SuspensionDuration.ofDays(7)).isEqualTo(SuspensionDuration.DAYS_7);
        assertThat(SuspensionDuration.ofDays(30)).isEqualTo(SuspensionDuration.DAYS_30);
        assertThat(SuspensionDuration.ofDays(null)).isEqualTo(SuspensionDuration.PERMANENT);
    }

    @Test
    void should_throw_InvalidInputException_when_days_is_unsupported() {
        // when & then
        assertThatThrownBy(() -> SuspensionDuration.ofDays(3)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> SuspensionDuration.ofDays(0)).isInstanceOf(InvalidInputException.class);
    }
}
