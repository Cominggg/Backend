package com.Coming.Backend.concert.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ConcertStatusTest {

    @ParameterizedTest
    @EnumSource(value = ConcertStatus.class, names = {"EXCLUDED", "PENDING"})
    void should_return_true_when_status_is_hidden(ConcertStatus status) {
        // given
        // when
        boolean isHidden = status.isHidden();

        // then
        assertThat(isHidden).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ConcertStatus.class, names = {"UPCOMING", "ONGOING", "ENDED", "CANCELLED"})
    void should_return_false_when_status_is_visible(ConcertStatus status) {
        // given
        // when
        boolean isHidden = status.isHidden();

        // then
        assertThat(isHidden).isFalse();
    }
}
