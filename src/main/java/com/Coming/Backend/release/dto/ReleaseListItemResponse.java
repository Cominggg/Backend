package com.Coming.Backend.release.dto;

import com.Coming.Backend.release.entity.ReleaseGroup;

import java.time.LocalDate;

public record ReleaseListItemResponse(
        Long id,
        String coverUrl,
        String artistName,
        String title,
        String type,
        LocalDate releaseDate,
        String spotifyId
) {
    public static ReleaseListItemResponse of(ReleaseGroup release, String artistName) {
        return new ReleaseListItemResponse(
                release.getId(),
                release.getCoverUrl(),
                artistName,
                release.getTitle(),
                release.getType(),
                release.getFirstReleaseDate(),
                release.getSpotifyId()
        );
    }
}
