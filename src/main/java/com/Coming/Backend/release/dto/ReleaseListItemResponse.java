package com.Coming.Backend.release.dto;

import com.Coming.Backend.release.entity.ReleaseGroup;

import java.time.LocalDate;

public record ReleaseListItemResponse(
        Long id,
        String coverUrl,
        String artistName,
        String artistKoreanName,
        String title,
        String type,
        LocalDate releaseDate,
        String spotifyId,
        Double averageRating,
        long ratingCount
) {
    public static ReleaseListItemResponse of(ReleaseGroup release, String artistName, String artistKoreanName,
                                              Double averageRating, long ratingCount) {
        return new ReleaseListItemResponse(
                release.getId(),
                release.getCoverUrl(),
                artistName,
                artistKoreanName,
                release.getTitle(),
                release.getType(),
                release.getFirstReleaseDate(),
                release.getSpotifyId(),
                averageRating,
                ratingCount
        );
    }
}
