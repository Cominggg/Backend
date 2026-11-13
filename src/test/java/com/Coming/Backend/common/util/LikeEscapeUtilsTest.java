package com.Coming.Backend.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LikeEscapeUtilsTest {

    @Test
    void should_escape_percent_and_underscore_when_escape() {
        // when
        String result = LikeEscapeUtils.escape("100%_a");

        // then
        assertThat(result).isEqualTo("100\\%\\_a");
    }

    @Test
    void should_escape_backslash_before_wildcards_when_escape() {
        // when
        String result = LikeEscapeUtils.escape("a\\%");

        // then
        assertThat(result).isEqualTo("a\\\\\\%");
    }

    @Test
    void should_return_same_string_when_no_special_characters() {
        // when
        String result = LikeEscapeUtils.escape("아이유 IU");

        // then
        assertThat(result).isEqualTo("아이유 IU");
    }

    @Test
    void should_lowercase_escape_and_wrap_with_percent_when_contains_pattern() {
        // when
        String result = LikeEscapeUtils.containsPattern("A_B%");

        // then
        assertThat(result).isEqualTo("%a\\_b\\%%");
    }

    @Test
    void should_lowercase_with_root_locale_when_contains_pattern() {
        // when
        String result = LikeEscapeUtils.containsPattern("TITLE");

        // then
        assertThat(result).isEqualTo("%title%");
    }
}
