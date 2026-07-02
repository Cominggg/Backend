package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record PipelineConcertCollectResult(
        boolean success,
        @JsonAlias("concert_id") Long concertId,
        String title,
        @JsonAlias("matched_artists") List<PipelineMatchedArtistResult> matchedArtists,
        String reason
) {
}
