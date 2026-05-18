package com.Coming.Backend.artist.dto;

import java.time.LocalDate;

public record ArtistConcertResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String venue,
        String status
) {
}
