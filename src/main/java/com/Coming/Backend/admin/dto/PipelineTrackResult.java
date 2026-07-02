package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PipelineTrackResult(
        Integer position,
        @JsonProperty("song_name") String songName,
        String info
) {
}
