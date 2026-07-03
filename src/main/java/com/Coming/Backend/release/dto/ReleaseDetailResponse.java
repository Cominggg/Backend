package com.Coming.Backend.release.dto;

import com.Coming.Backend.release.entity.ReleaseGroup;

import java.time.LocalDate;
import java.util.List;

public record ReleaseDetailResponse(
        Long id,
        String title,
        String type,
        LocalDate releaseDate,
        String coverUrl,
        String label,
        Integer totalTracks,
        Long artistId,
        String artistName,
        String artistKoreanName,
        String spotifyId,
        List<TrackDto> tracks
) {
    public static ReleaseDetailResponse of(ReleaseGroup release, String artistName, String artistKoreanName, List<TrackDto> tracks) {
        return new ReleaseDetailResponse(
                release.getId(),
                release.getTitle(),
                release.getType(),
                release.getFirstReleaseDate(),
                release.getCoverUrl(),
                release.getLabel(),
                release.getTotalTracks(),
                release.getArtistId(),
                artistName,
                artistKoreanName,
                release.getSpotifyId(),
                tracks
        );
    }
}
