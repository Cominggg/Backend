package com.Coming.Backend.admin.dto;

import com.Coming.Backend.concert.entity.ConcertStatus;
import jakarta.validation.constraints.NotNull;

public record AdminConcertStateUpdateRequest(
        @NotNull
        ConcertStatus status,

        String reason
) {
}
