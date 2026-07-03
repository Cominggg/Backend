package com.Coming.Backend.user.dto;

import java.time.LocalDate;

public record ConcertHistoryResponse(
        Long id,
        String artistName,
        String artistKoreanName,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String venue,
        String status
) {
}
