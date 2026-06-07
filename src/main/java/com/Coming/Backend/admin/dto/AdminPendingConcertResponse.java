package com.Coming.Backend.admin.dto;

import com.Coming.Backend.concert.entity.Concert;

import java.time.LocalDate;
import java.util.List;

public record AdminPendingConcertResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String venueName,
        String posterUrl,
        List<AdminCandidateArtistResponse> candidates
) {
    public static AdminPendingConcertResponse of(Concert concert, List<AdminCandidateArtistResponse> candidates) {
        return new AdminPendingConcertResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getStartDate(),
                concert.getEndDate(),
                concert.getVenueName(),
                concert.getPosterUrl(),
                candidates
        );
    }
}
