package com.Coming.Backend.concert.dto;

import com.Coming.Backend.concert.entity.ConcertStatus;

import java.time.LocalDate;

public record ConcertFilterRequest(
        LocalDate dateFrom,
        LocalDate dateTo,
        Long artistId,
        ConcertStatus status
) {
}
