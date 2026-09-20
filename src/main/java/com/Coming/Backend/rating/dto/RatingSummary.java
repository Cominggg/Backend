package com.Coming.Backend.rating.dto;

public record RatingSummary(
        Double averageRating,
        long ratingCount
) {
}
