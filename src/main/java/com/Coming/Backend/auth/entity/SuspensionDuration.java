package com.Coming.Backend.auth.entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
     * @return 정지 만료 시각. 영구 정지면 null
     */
    public LocalDateTime untilFrom(LocalDateTime now) {
        if (this == PERMANENT) {
            return null;
        }
        return now.truncatedTo(ChronoUnit.SECONDS).plusDays(days);
    }
}
