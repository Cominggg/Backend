package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record PipelineArtistCollectResult(
        boolean success,
        @JsonAlias("artist_id") Long artistId,
        String mbid,
        String name,
        @JsonAlias("image_url") String imageUrl,
        List<PipelineAliasResult> aliases,
        String reason
) {
}
