package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record PipelineMatchedArtistResult(
        @JsonAlias("artist_id") Long artistId,
        String name
) {
}
