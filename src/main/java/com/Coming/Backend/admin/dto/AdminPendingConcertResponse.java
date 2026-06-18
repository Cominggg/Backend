package com.Coming.Backend.admin.dto;

import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertBookingLink;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminPendingConcertResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String venueName,
        String posterUrl,
        LocalDateTime ticketOpenAt,
        List<BookingLinkDto> bookingLinks,
        List<AdminCandidateArtistResponse> candidates
) {
    public record BookingLinkDto(String name, String url) {}

    public static AdminPendingConcertResponse of(Concert concert,
                                                  List<ConcertBookingLink> links,
                                                  List<AdminCandidateArtistResponse> candidates) {
        return new AdminPendingConcertResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getStartDate(),
                concert.getEndDate(),
                concert.getVenueName(),
                concert.getPosterUrl(),
                concert.getTicketOpenAt(),
                links.stream().map(l -> new BookingLinkDto(l.getName(), l.getUrl())).toList(),
                candidates
        );
    }
}
