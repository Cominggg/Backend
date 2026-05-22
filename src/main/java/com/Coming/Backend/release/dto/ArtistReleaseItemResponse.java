package com.Coming.Backend.release.dto;

import com.Coming.Backend.release.entity.ReleaseGroup;

import java.time.LocalDate;
import java.util.List;

public record ArtistReleaseItemResponse(
        Long id,
        String title,
        String type,
        LocalDate releaseDate,
        String coverUrl,
        List<TrackDto> tracks
) {
    public static ArtistReleaseItemResponse of(ReleaseGroup release, List<TrackDto> tracks) {
        return new ArtistReleaseItemResponse(
                release.getId(),
                release.getTitle(),
                release.getType(),
                release.getFirstReleaseDate(),
                release.getCoverUrl(),
                tracks
        );
    }
}
