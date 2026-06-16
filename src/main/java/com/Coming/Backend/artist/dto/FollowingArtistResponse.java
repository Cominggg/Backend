package com.Coming.Backend.artist.dto;

public record FollowingArtistResponse(
        Long id,
        String name,
        String imageUrl,
        boolean hasUpcomingConcert,
        boolean isFollowing
) {
}
