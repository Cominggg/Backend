package com.Coming.Backend.concert.dto;

import com.Coming.Backend.concert.entity.ConcertStatus;

import java.time.LocalDate;

public record ConcertSummaryResponse(
        Long id,
        String posterUrl,
        String artistName,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String venue,
        ConcertStatus status,
        boolean isInCalendar
) {
}
