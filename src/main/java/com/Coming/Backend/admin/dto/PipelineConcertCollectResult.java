package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PipelineConcertCollectResult(
        boolean success,
        @JsonProperty("concert_id") Long concertId,
        String title,
        @JsonProperty("matched_artists") List<PipelineMatchedArtistResult> matchedArtists,
        String reason
) {
}
