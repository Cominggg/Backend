package com.Coming.Backend.concert.dto;

import com.Coming.Backend.concert.entity.ConcertStatus;

import java.time.LocalDate;
import java.util.List;

public record ConcertDetailResponse(
        Long id,
        String posterUrl,
        List<String> imageUrls,
        String artistName,
        Long artistId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String venue,
        ConcertStatus status,
        String price,
        boolean isInCalendar,
        List<TicketLinkDto> ticketLinks
) {
}
