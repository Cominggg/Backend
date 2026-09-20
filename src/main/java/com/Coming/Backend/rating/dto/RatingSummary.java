package com.Coming.Backend.rating.dto;

public record RatingSummary(
        Double averageRating,
        long ratingCount
) {
    public static RatingSummary empty() {
        return new RatingSummary(null, 0);
    }
}
