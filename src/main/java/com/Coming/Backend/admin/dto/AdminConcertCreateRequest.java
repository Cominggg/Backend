package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminConcertCreateRequest(
        @NotBlank
        String title,

        String cast,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        @NotBlank
        String venueName,

        String posterUrl,
        List<String> imageUrls,
        String price,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime ticketOpenAt,

        @Valid List<BookingLinkRequest> bookingLinks
) {
}
