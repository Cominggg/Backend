package com.Coming.Backend.admin.dto;

import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertBookingLink;
import com.Coming.Backend.concert.entity.ConcertStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminConcertDetailResponse(
        Long id,
        String title,
        String cast,
        LocalDate startDate,
        LocalDate endDate,
        String venueName,
        String posterUrl,
        String price,
        ConcertStatus status,
        LocalDateTime ticketOpenAt,
        List<BookingLinkDto> bookingLinks
) {
    public record BookingLinkDto(String name, String url) {}

    public static AdminConcertDetailResponse of(Concert concert, List<ConcertBookingLink> links) {
        return new AdminConcertDetailResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getCast(),
                concert.getStartDate(),
                concert.getEndDate(),
                concert.getVenueName(),
                concert.getPosterUrl(),
                concert.getPrice(),
                concert.getStatus(),
                concert.getTicketOpenAt(),
                links.stream().map(l -> new BookingLinkDto(l.getName(), l.getUrl())).toList()
        );
    }
}
