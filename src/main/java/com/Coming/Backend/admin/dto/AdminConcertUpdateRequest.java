package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminConcertUpdateRequest(
        String title,
        String cast,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        String venueName,
        String posterUrl,
        List<String> imageUrls,
        String price,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime ticketOpenAt,

        @Valid List<BookingLinkRequest> bookingLinks
) {
}
