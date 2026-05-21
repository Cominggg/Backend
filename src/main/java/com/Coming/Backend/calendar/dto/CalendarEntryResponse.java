package com.Coming.Backend.calendar.dto;

import java.time.LocalDate;

public record CalendarEntryResponse(
        Long concertId,
        String artistName,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String posterUrl,
        String venue,
        boolean isInCalendar
) {
}
