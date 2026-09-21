package com.Coming.Backend.rating.dto;

import java.math.BigDecimal;

public record RatingMeResponse(
        BigDecimal score
) {
    public static RatingMeResponse empty() {
        return new RatingMeResponse(null);
    }
}
