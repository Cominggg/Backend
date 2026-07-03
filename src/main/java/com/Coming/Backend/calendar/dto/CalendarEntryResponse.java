package com.Coming.Backend.calendar.dto;

import com.Coming.Backend.concert.dto.ConcertArtistDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CalendarEntryResponse(
        Long concertId,
        String type,
        List<ConcertArtistDto> artists,
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
