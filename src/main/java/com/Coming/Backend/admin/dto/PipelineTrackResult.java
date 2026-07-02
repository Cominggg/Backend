package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record PipelineTrackResult(
        Integer position,
        @JsonAlias("song_name") String songName,
        String info
) {
}
