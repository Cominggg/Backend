package com.Coming.Backend.auth.entity;

import com.Coming.Backend.common.exception.InvalidInputException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * 회원 정지 기간. 응답·리다이렉트에 초 단위로 내려가므로 만료 시각은 초 미만을 버린다.
 */
public enum SuspensionDuration {
    DAYS_7(7), DAYS_30(30), PERMANENT(0);

    private final int days;

    SuspensionDuration(int days) {
        this.days = days;
    }

    /**
     * 관리자 요청의 정지 일수를 기간으로 바꾼다.
     *
     * @param days 7, 30 또는 null(영구 정지)
     * @throws InvalidInputException 지원하지 않는 일수인 경우
     */
    public static SuspensionDuration ofDays(Integer days) {
        if (days == null) {
            return PERMANENT;
        }
        return Arrays.stream(values())
                .filter(duration -> duration != PERMANENT && duration.days == days)
                .findFirst()
                .orElseThrow(InvalidInputException::new);
    }

    /**
     * @return 정지 만료 시각. 영구 정지면 null
     */
    public LocalDateTime untilFrom(LocalDateTime now) {
        if (this == PERMANENT) {
            return null;
        }
        return now.truncatedTo(ChronoUnit.SECONDS).plusDays(days);
    }
}
