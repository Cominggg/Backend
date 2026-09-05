package com.Coming.Backend.common.util;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortPropertyValidatorTest {

    private static final Set<String> ALLOWED_PROPERTIES = Set.of("name", "createdAt");

    @Test
    void should_not_throw_when_all_sort_properties_are_allowed() {
        // given
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name", "createdAt"));

        // when & then
        assertThatCode(() -> SortPropertyValidator.validate(pageable, ALLOWED_PROPERTIES))
                .doesNotThrowAnyException();
    }

    @Test
    void should_throw_invalid_input_exception_when_disallowed_property_included() {
        // given
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name", "secretField"));

        // when & then
        assertThatThrownBy(() -> SortPropertyValidator.validate(pageable, ALLOWED_PROPERTIES))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(ErrorCode.INVALID_INPUT.getMessage());
    }

    @Test
    void should_not_throw_when_pageable_has_no_sort() {
        // given
        Pageable pageable = PageRequest.of(0, 20, Sort.unsorted());

        // when & then
        assertThatCode(() -> SortPropertyValidator.validate(pageable, ALLOWED_PROPERTIES))
                .doesNotThrowAnyException();
    }

    @Test
    void should_not_throw_when_only_one_sort_property_given() {
        // given
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name"));

        // when & then
        assertThatCode(() -> SortPropertyValidator.validateSingleSort(pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void should_throw_invalid_input_exception_when_multiple_sort_properties_given() {
        // given
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name", "createdAt"));

        // when & then
        assertThatThrownBy(() -> SortPropertyValidator.validateSingleSort(pageable))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(ErrorCode.INVALID_INPUT.getMessage());
    }
}
