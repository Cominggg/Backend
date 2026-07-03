package com.Coming.Backend.artist.dto;

public record ArtistSummaryResponse(
        Long id,
        String name,
        String koreanName,
        String imageUrl,
        boolean hasUpcomingConcert,
        boolean isFollowing,
        String spotifyUrl
) {
}
