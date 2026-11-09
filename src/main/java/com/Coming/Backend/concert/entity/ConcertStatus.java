package com.Coming.Backend.concert.entity;

import java.util.EnumSet;
import java.util.Set;

public enum ConcertStatus {
    UPCOMING, ONGOING, ENDED, CANCELLED, EXCLUDED, PENDING;

    /**
     * 사용자에게 노출하지 않는 상태. 관리자가 제외했거나(EXCLUDED) 아직 승인되지 않은(PENDING) 공연이다.
     */
    public static final Set<ConcertStatus> HIDDEN = EnumSet.of(EXCLUDED, PENDING);

    public boolean isHidden() {
        return HIDDEN.contains(this);
    }
}
