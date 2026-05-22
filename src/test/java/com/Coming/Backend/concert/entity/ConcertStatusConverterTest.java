package com.Coming.Backend.concert.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConcertStatusConverterTest {

    private ConcertStatusConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ConcertStatusConverter();
    }

    @ParameterizedTest(name = "{0} → \"{1}\"")
    @CsvSource({
        "UPCOMING,  UPCOMING",
        "ONGOING,   ONGOING",
        "ENDED,     ENDED",
        "CANCELLED, CANCELLED"
    })
    void should_return_english_string_when_enum_given(ConcertStatus status, String expected) {
        // when
        String dbValue = converter.convertToDatabaseColumn(status);

        // then
        assertThat(dbValue).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource({
        "UPCOMING,  UPCOMING",
        "ONGOING,   ONGOING",
        "ENDED,     ENDED",
        "CANCELLED, CANCELLED"
    })
    void should_return_enum_when_english_string_given(String dbValue, ConcertStatus expected) {
        // when
        ConcertStatus status = converter.convertToEntityAttribute(dbValue);

        // then
        assertThat(status).isEqualTo(expected);
    }

    @Test
    void should_throw_illegal_argument_exception_when_unknown_db_value_given() {
        // when & then
        assertThatThrownBy(() -> converter.convertToEntityAttribute("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown ConcertStatus value: UNKNOWN");
    }
}
