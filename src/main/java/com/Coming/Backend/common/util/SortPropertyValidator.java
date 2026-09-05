package com.Coming.Backend.common.util;

import com.Coming.Backend.common.exception.InvalidInputException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class SortPropertyValidator {

    private SortPropertyValidator() {}

    /**
     * Pageable의 정렬 필드가 허용 목록에 포함되는지 검증한다.
     *
     * @throws InvalidInputException 허용되지 않은 정렬 필드가 포함된 경우
     */
    public static void validate(Pageable pageable, Set<String> allowedProperties) {
        boolean hasInvalidProperty = pageable.getSort().stream()
                .map(Sort.Order::getProperty)
                .anyMatch(property -> !allowedProperties.contains(property));
        if (hasInvalidProperty) {
            throw new InvalidInputException();
        }
    }

    /**
     * Pageable의 정렬 필드가 하나만 지정됐는지 검증한다.
     *
     * @throws InvalidInputException 정렬 필드가 둘 이상인 경우
     */
    public static void validateSingleSort(Pageable pageable) {
        if (pageable.getSort().stream().count() > 1) {
            throw new InvalidInputException();
        }
    }
}
