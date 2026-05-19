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
        "UPCOMING, 공연예정",
        "ONGOING,  공연중",
        "ENDED,    공연완료",
        "CANCELLED,공연취소"
    })
    void should_return_korean_string_when_enum_given(ConcertStatus status, String expected) {
        // given — status, expected 는 파라미터로 주입

        // when
        String dbValue = converter.convertToDatabaseColumn(status);

        // then
        assertThat(dbValue).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource({
        "공연예정, UPCOMING",
        "공연중,   ONGOING",
        "공연완료, ENDED",
        "공연취소, CANCELLED"
    })
    void should_return_enum_when_korean_string_given(String dbValue, ConcertStatus expected) {
        // given — dbValue, expected 는 파라미터로 주입

        // when
        ConcertStatus status = converter.convertToEntityAttribute(dbValue);

        // then
        assertThat(status).isEqualTo(expected);
    }

    @Test
    void should_throw_illegal_argument_exception_when_unknown_db_value_given() {
        // given
        String unknownValue = "알수없는상태";

        // when & then
        assertThatThrownBy(() -> converter.convertToEntityAttribute(unknownValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown ConcertStatus value: " + unknownValue);
    }
}
