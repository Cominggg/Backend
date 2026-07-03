package com.Coming.Backend.artist.dto;

import java.util.List;

public record ArtistDetailResponse(
        Long id,
        String name,
        String koreanName,
        String imageUrl,
        boolean hasUpcomingConcert,
        boolean isFollowing,
        long followersCount,
        List<ArtistLinkDto> links
) {
}
