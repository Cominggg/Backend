package com.Coming.Backend.admin.dto;

import com.Coming.Backend.concert.entity.ConcertStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record AdminConcertCreateRequest(
        @NotBlank
        String kopisId,

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

        String venueAddress,

        String posterUrl,

        String price,

        @NotNull
        ConcertStatus status,

        List<Long> artistIds
) {
}
