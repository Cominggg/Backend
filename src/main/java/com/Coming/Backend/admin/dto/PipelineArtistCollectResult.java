package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PipelineArtistCollectResult(
        boolean success,
        @JsonProperty("artist_id") Long artistId,
        String mbid,
        String name,
        @JsonProperty("image_url") String imageUrl,
        List<PipelineAliasResult> aliases,
        String reason
) {
}
