package com.Coming.Backend.user.dto;

import com.Coming.Backend.calendar.dto.CalendarEntryResponse;

public record MySummaryResponse(
        long followingCount,
        long upcomingCount,
        // 예정·진행 중 공연 중 가장 빠른 1건. 없으면 null
        CalendarEntryResponse nextConcert,
        long historyCount,
        long inProgressInquiryCount
) {
}
