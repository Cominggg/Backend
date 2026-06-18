package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

public record AdminConcertApproveRequest(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime ticketOpenAt,

        @Valid List<BookingLinkRequest> bookingLinks
) {
}
