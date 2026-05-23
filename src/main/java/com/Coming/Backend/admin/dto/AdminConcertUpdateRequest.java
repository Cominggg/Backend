package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record AdminConcertUpdateRequest(
        String title,
        String cast,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        String venueName,
        String venueAddress,
        String posterUrl,
        String price,
        List<BookingLinkRequest> bookingLinks
) {
}
