package com.Coming.Backend.rating.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RatingUpsertRequest(
        @NotNull
        @DecimalMin("0.5")
        @DecimalMax("5.0")
        BigDecimal score
) {
}
