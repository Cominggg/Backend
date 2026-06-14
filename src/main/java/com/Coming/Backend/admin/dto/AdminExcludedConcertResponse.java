package com.Coming.Backend.admin.dto;

import com.Coming.Backend.concert.entity.Concert;

import java.time.LocalDate;
import java.util.List;

public record AdminExcludedConcertResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String venueName,
        String posterUrl,
        List<AdminExcludedArtistResponse> artists
) {
    public static AdminExcludedConcertResponse of(Concert concert, List<AdminExcludedArtistResponse> artists) {
        return new AdminExcludedConcertResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getStartDate(),
                concert.getEndDate(),
                concert.getVenueName(),
                concert.getPosterUrl(),
                artists
        );
    }
}
