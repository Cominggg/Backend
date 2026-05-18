package com.Coming.Backend.artist.dto;

import java.time.LocalDate;
import java.util.List;

public record ArtistDetailResponse(
        Long id,
        String name,
        String imageUrl,
        boolean hasUpcomingConcert,
        boolean isFollowing,
        long followersCount,
        LocalDate debutDate,
        List<ArtistLinkDto> links
) {
}
