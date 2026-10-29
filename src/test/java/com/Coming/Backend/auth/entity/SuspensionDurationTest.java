package com.Coming.Backend.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

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
}
