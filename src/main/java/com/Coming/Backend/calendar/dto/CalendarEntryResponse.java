package com.Coming.Backend.calendar.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CalendarEntryResponse(
        Long concertId,
        String type,
        String artistName,
        String artistKoreanName,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String posterUrl,
        String venue,
        boolean isInCalendar,
        LocalDateTime ticketOpenAt
) {
}
