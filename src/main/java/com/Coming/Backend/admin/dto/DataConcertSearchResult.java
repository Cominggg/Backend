package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDate;

public record DataConcertSearchResult(
        @JsonAlias("kopis_id") String kopisId,
        String title,
        @JsonAlias("start_date") LocalDate startDate,
        @JsonAlias("end_date") LocalDate endDate,
        String venue,
        String url
) {
}
