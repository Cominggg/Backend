package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PipelineMatchedArtistResult(
        @JsonProperty("artist_id") Long artistId,
        String name
) {
}
