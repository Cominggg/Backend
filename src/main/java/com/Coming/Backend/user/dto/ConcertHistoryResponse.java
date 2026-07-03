package com.Coming.Backend.user.dto;

import com.Coming.Backend.concert.dto.ConcertArtistDto;

import java.time.LocalDate;
import java.util.List;

public record ConcertHistoryResponse(
        Long id,
        List<ConcertArtistDto> artists,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String venue,
        String status
) {
}
